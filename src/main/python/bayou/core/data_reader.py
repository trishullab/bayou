# Copyright 2017 Rice University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import print_function
import json
import numpy as np
import random
from collections import Counter

from bayou.core.utils import C0, CHILD_EDGE, SIBLING_EDGE


class Reader():
    def __init__(self, clargs, config):
        self.config = config

        # read the raw evidences and targets
        print('Reading data file...')
        raw_evidences, raw_targets = self.read_data(clargs.input_file[0])
        raw_evidences = [[raw_evidence[i] for raw_evidence in raw_evidences] for i, ev in
                         enumerate(config.evidence)]

        # align with number of batches
        config.num_batches = int(len(raw_targets) / config.batch_size)
        assert config.num_batches > 0, 'Not enough data'
        sz = config.num_batches * config.batch_size
        for i in range(len(raw_evidences)):
            raw_evidences[i] = raw_evidences[i][:sz]
        raw_targets = raw_targets[:sz]

        # setup input and target chars/vocab
        if clargs.continue_from is None:
            counts = Counter([n for path in raw_targets for (n, _) in path])
            counts[C0] = 1
            config.decoder.chars = sorted(counts.keys(), key=lambda w: counts[w], reverse=True)
            config.decoder.vocab = dict(zip(config.decoder.chars, range(len(config.decoder.chars))))
            config.decoder.vocab_size = len(config.decoder.vocab)

        # wrangle the evidences and targets into numpy arrays
        self.inputs = [ev.wrangle(data) for ev, data in zip(config.evidence, raw_evidences)]
        self.nodes = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)
        self.edges = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.bool)
        self.targets = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)
        for i, path in enumerate(raw_targets):
            self.nodes[i, :len(path)] = list(map(config.decoder.vocab.get, [p[0] for p in path]))
            self.edges[i, :len(path)] = [p[1] == CHILD_EDGE for p in path]
            self.targets[i, :len(path)-1] = self.nodes[i, 1:len(path)]  # shifted left by one

        # split into batches
        self.inputs = [np.split(ev_data, config.num_batches, axis=0) for ev_data in self.inputs]
        self.nodes = np.split(self.nodes, config.num_batches, axis=0)
        self.edges = np.split(self.edges, config.num_batches, axis=0)
        self.targets = np.split(self.targets, config.num_batches, axis=0)

        # reset batches
        self.reset_batches()

    def get_ast_paths(self, js, idx=0):
        cons_calls = []
        i = idx
        while i < len(js):
            if js[i]['node'] == 'DAPICall':
                cons_calls.append((js[i]['_call'], SIBLING_EDGE))
            else:
                break
            i += 1
        if i == len(js):
            cons_calls.append(('STOP', SIBLING_EDGE))
            return [cons_calls]
        node_type = js[i]['node']

        if node_type == 'DBranch':
            pC = self.get_ast_paths(js[i]['_cond'])  # will have at most 1 "path"
            assert len(pC) <= 1
            p1 = self.get_ast_paths(js[i]['_then'])
            p2 = self.get_ast_paths(js[i]['_else'])
            p = [p1[0] + path for path in p2] + p1[1:]
            pv = [cons_calls + [('DBranch', CHILD_EDGE)] + pC[0] + path for path in p]
            p = self.get_ast_paths(js, i+1)
            ph = [cons_calls + [('DBranch', SIBLING_EDGE)] + path for path in p]
            return ph + pv

        if node_type == 'DExcept':
            p1 = self.get_ast_paths(js[i]['_try'])
            p2 = self.get_ast_paths(js[i]['_catch'])
            p = [p1[0] + path for path in p2] + p1[1:]
            pv = [cons_calls + [('DExcept', CHILD_EDGE)] + path for path in p]
            p = self.get_ast_paths(js, i+1)
            ph = [cons_calls + [('DExcept', SIBLING_EDGE)] + path for path in p]
            return ph + pv

        if node_type == 'DLoop':
            pC = self.get_ast_paths(js[i]['_cond'])  # will have at most 1 "path"
            assert len(pC) <= 1
            p = self.get_ast_paths(js[i]['_body'])
            pv = [cons_calls + [('DLoop', CHILD_EDGE)] + pC[0] + path for path in p]
            p = self.get_ast_paths(js, i+1)
            ph = [cons_calls + [('DLoop', SIBLING_EDGE)] + path for path in p]
            return ph + pv

    def read_data(self, filename):
        with open(filename) as f:
            js = json.load(f)
        evidences, targets = [], []
        ignored, done = 0, 0

        for program in js['programs']:
            if 'ast' not in program:
                continue
            try:
                evidence = [ev.read_data_point(program) for ev in self.config.evidence]
                ast_paths = self.get_ast_paths(program['ast']['_nodes'])
                for path in ast_paths:
                    path.insert(0, ('DSubTree', CHILD_EDGE))
                    assert len(path) <= self.config.decoder.max_ast_depth
                    evidences.append(evidence)
                    targets.append(path)
            except AssertionError:
                ignored += 1
            done += 1
        print('{:8d} programs in training data'.format(done))
        print('{:8d} programs ignored by given config'.format(ignored))

        # randomly shuffle to avoid bias towards initial data points during training
        data_points = list(zip(evidences, targets))
        random.shuffle(data_points)
        evidences, targets = zip(*data_points)

        return evidences, targets

    def next_batch(self):
        batch = next(self.batches)
        n, e, y = batch[:3]
        ev_data = batch[3:]

        # reshape the batch into required format
        rn = np.transpose(n)
        re = np.transpose(e)

        return ev_data, rn, re, y

    def reset_batches(self):
        self.batches = iter(zip(self.nodes, self.edges, self.targets, *self.inputs))
