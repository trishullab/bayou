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
import ijson.backends.yajl2_cffi as ijson
import numpy as np
import random
import os
import pickle
from collections import Counter
import gc

from bayou.models.low_level_evidences.utils import C0, gather_calls, chunks, get_available_gpus, dump_config
from bayou.models.low_level_evidences.node import Node
CHILD_EDGE = True
SIBLING_EDGE = False


class TooLongPathError(Exception):
    pass


class InvalidSketchError(Exception):
    pass


class Reader():
    def __init__(self, clargs, config, infer=False, dataIsThere=False):
        self.infer = infer
        self.config = config

        if clargs.continue_from is not None or dataIsThere:
            with open('data/inputs.txt', 'rb') as f:
                self.inputs = pickle.load(f)
            with open('data/nodes.txt', 'rb') as f:
                self.nodes = pickle.load(f)
            with open('data/parents.txt', 'rb') as f:
                self.parents = pickle.load(f)
            with open('data/edges.txt', 'rb') as f:
                self.edges = pickle.load(f)
            with open('data/targets.txt', 'rb') as f:
                self.targets = pickle.load(f)

            jsconfig = dump_config(config)
            with open(os.path.join(clargs.save, 'config.json'), 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)

            if infer:
                self.js_programs = []
                with open('data/js_programs.json', 'rb') as f:
                    for program in ijson.items(f, 'programs.item'):
                        self.js_programs.append(program)
            config.num_batches = int(len(self.nodes) / config.batch_size)

        else:
            random.seed(12)
            # read the raw evidences and targets
            print('Reading data file...')
            raw_evidences, raw_targets, js_programs = self.read_data(clargs.input_file[0], infer, save=clargs.save)
            print('Done!')
            raw_evidences = [[raw_evidence[i] for raw_evidence in raw_evidences] for i, ev in
                             enumerate(config.evidence)]


            config.num_batches = int(len(raw_targets) / config.batch_size)
            assert config.num_batches > 0, 'Not enough data'

            sz = config.num_batches * config.batch_size
            for i in range(len(raw_evidences)):
                raw_evidences[i] = raw_evidences[i][:sz]
            raw_targets = raw_targets[:sz]

        # setup input and target chars/vocab
            if clargs.continue_from is None:
                config.decoder.vocab = self.CallMapDict
                config.decoder.vocab_size = len(self.CallMapDict)
                # adding the same variables for reverse Encoder

            # wrangle the evidences and targets into numpy arrays
            self.inputs = [ev.wrangle(data) for ev, data in zip(config.evidence, raw_evidences)]
            self.nodes = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)
            self.parents = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)
            self.edges = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.bool)
            self.targets = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)

            for i, path in enumerate(raw_targets):
                self.nodes[i, :len(path)] = [p[0] for p in path]
                self.parents[i, :len(path)] = [p[1] for p in path]
                self.edges[i, :len(path)] = [p[2] for p in path]
                self.targets[i, :len(path)-1] = self.nodes[i, 1:len(path)]  # shifted left by one

            self.js_programs = js_programs


            with open('data/inputs.txt', 'wb') as f:
                pickle.dump(self.inputs, f)
            with open('data/nodes.txt', 'wb') as f:
                pickle.dump(self.nodes, f)
            with open('data/edges.txt', 'wb') as f:
                pickle.dump(self.edges, f)
            with open('data/parents.txt', 'wb') as f:
                pickle.dump(self.parents, f)
            with open('data/targets.txt', 'wb') as f:
                pickle.dump(self.targets, f)
            with open('data/js_programs.json', 'w') as f:
                json.dump({'programs': self.js_programs}, fp=f, indent=2)
            jsconfig = dump_config(config)
            with open(os.path.join(clargs.save, 'config.json'), 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)
            with open('data/config.json', 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)


    def get_ast_paths(self, js, idx=0):
        #print (idx)
        cons_calls = []
        i = idx
        curr_Node = None
        head = None
        while i < len(js):
            if js[i]['node'] == 'DAPICall':
                cons_calls.append((js[i]['_call'], SIBLING_EDGE))
                if curr_Node == None:
                    curr_Node = Node(js[i]['_call'])
                    head = curr_Node
                else:
                    curr_Node.sibling = Node(js[i]['_call'])
                    curr_Node = curr_Node.sibling
            else:
                break
            i += 1
        if i == len(js):
            cons_calls.append(('STOP', SIBLING_EDGE))
            if curr_Node == None:
                curr_Node = Node('STOP')
                head = curr_Node
            else:
                curr_Node.sibling = Node('STOP')
                curr_Node = curr_Node.sibling
            return head, [cons_calls]

        node_type = js[i]['node']

        if node_type == 'DBranch':

            if curr_Node == None:
                curr_Node = Node('DBranch')
                head = curr_Node
            else:
                curr_Node.sibling = Node('DBranch')
                curr_Node = curr_Node.sibling

            nodeC, pC = self.get_ast_paths( js[i]['_cond'])  # will have at most 1 "path"
            assert len(pC) <= 1
            nodeC_last = nodeC.iterateHTillEnd(nodeC)
            nodeC_last.sibling, p1 = self.get_ast_paths( js[i]['_then'])
            nodeE, p2 = self.get_ast_paths( js[i]['_else'])
            curr_Node.child = Node(nodeC.val, child=nodeE, sibling=nodeC.sibling)

            p = [p1[0] + path for path in p2] + p1[1:]
            pv = [cons_calls + [('DBranch', CHILD_EDGE)] + pC[0] + path for path in p]


            nodeS, p = self.get_ast_paths( js, i+1)
            ph = [cons_calls + [('DBranch', SIBLING_EDGE)] + path for path in p]
            curr_Node.sibling = nodeS

            return head, ph + pv

        if node_type == 'DExcept':
            if curr_Node == None:
                curr_Node = Node('DExcept')
                head = curr_Node
            else:
                curr_Node.sibling = Node('DExcept')
                curr_Node = curr_Node.sibling

            nodeT , p1 = self.get_ast_paths( js[i]['_try'])
            nodeC , p2 =  self.get_ast_paths( js[i]['_catch'] )
            p = [p1[0] + path for path in p2] + p1[1:]

            curr_Node.child = Node(nodeT.val, child=nodeC, sibling=nodeT.sibling)
            pv = [cons_calls + [('DExcept', CHILD_EDGE)] + path for path in p]

            nodeS, p = self.get_ast_paths( js, i+1)
            ph = [cons_calls + [('DExcept', SIBLING_EDGE)] + path for path in p]
            curr_Node.sibling = nodeS
            return head, ph + pv

        if node_type == 'DLoop':
            if curr_Node == None:
                curr_Node = Node('DLoop')
                head = curr_Node
            else:
                curr_Node.sibling = Node('DLoop')
                curr_Node = curr_Node.sibling
            nodeC, pC = self.get_ast_paths( js[i]['_cond'])  # will have at most 1 "path"
            assert len(pC) <= 1
            nodeC_last = nodeC.iterateHTillEnd(nodeC)
            nodeC_last.sibling, p = self.get_ast_paths( js[i]['_body'])

            pv = [cons_calls + [('DLoop', CHILD_EDGE)] + pC[0] + path for path in p]
            nodeS, p = self.get_ast_paths( js, i+1)
            ph = [cons_calls + [('DLoop', SIBLING_EDGE)] + path for path in p]

            curr_Node.child = nodeC
            curr_Node.sibling = nodeS

            return head, ph + pv


    def _check_DAPICall_repeats(self, nodelist):
        """
        Checks if an API call node repeats in succession twice in a list of nodes

        :param nodelist: list of nodes to check
        :return: None
        :raise: InvalidSketchError if some API call node repeats, ValueError if a node is of invalid type
        """
        for i in range(1, len(nodelist)):
            node = nodelist[i]
            node_type = node['node']
            if node_type == 'DAPICall':
                if nodelist[i] == nodelist[i-1]:
                    raise InvalidSketchError
            elif node_type == 'DBranch':
                self._check_DAPICall_repeats(node['_cond'])
                self._check_DAPICall_repeats(node['_then'])
                self._check_DAPICall_repeats(node['_else'])
            elif node_type == 'DExcept':
                self._check_DAPICall_repeats(node['_try'])
                self._check_DAPICall_repeats(node['_catch'])
            elif node_type == 'DLoop':
                self._check_DAPICall_repeats(node['_cond'])
                self._check_DAPICall_repeats(node['_body'])
            else:
                raise ValueError('Invalid node type: ' + node)

    def validate_sketch_paths(self, program, ast_paths):
        """
        Checks if a sketch along with its paths is good training data:
        1. No API call should be repeated successively
        2. No path in the sketch should be of length more than max_ast_depth hyper-parameter
        3. No branch, loop or except should occur more than once along a single path

        :param program: the sketch
        :param ast_paths: paths in the sketch
        :return: None
        :raise: TooLongPathError or InvalidSketchError if sketch or its paths is invalid
        """
        #self._check_DAPICall_repeats(program['ast']['_nodes'])
        for path in ast_paths:
            if len(path) >= self.config.decoder.max_ast_depth:
                raise TooLongPathError
            nodes = [node for (node, edge) in path]
            if nodes.count('DBranch') > 1 or nodes.count('DLoop') > 1 or nodes.count('DExcept') > 1:
                raise TooLongPathError

    def read_data(self, filename, infer, save=None):
        # with open(filename) as f:
        #     js = json.load(f)
        f = open(filename , 'rb')

        data_points = []
        callmap = dict()
        file_ptr = dict()
        ignored, done = 0, 0
        if not infer:
            self.CallMapDict = dict()
            self.CallMapDict['STOP'] = 0
            count = 1
        else:
            self.CallMapDict = self.config.decoder.vocab
            count = self.config.decoder.vocab_size
        for program in ijson.items(f, 'programs.item'):
            if 'ast' not in program:
                continue
            try:
                evidences = [ev.read_data_point(program, infer) for ev in self.config.evidence]
                ast_node_graph, ast_paths = self.get_ast_paths(program['ast']['_nodes'])

                self.validate_sketch_paths(program, ast_paths)

                path = ast_node_graph.bfs()
                temp_arr = []
                for val in path:
                    nodeVal = val[0]
                    idVal   = val[1]
                    edgeVal = val[2]
                    if nodeVal not in self.CallMapDict:
                        if not infer:
                            self.CallMapDict[nodeVal] = count
                            temp_arr.append((count, idVal , edgeVal))
                            count += 1
                    else:
                        temp_arr.append((self.CallMapDict[nodeVal] , idVal, edgeVal))

                sample = dict()
                sample['file'] = program['file']
                sample['method'] = program['method']
                sample['body'] = program['body']
                data_points.append((evidences, temp_arr, sample))


                    #data_points.append((done - ignored, evidences, temp_arr, {}))
                calls = gather_calls(program['ast'])
                for call in calls:
                    if call['_call'] not in callmap:
                        callmap[call['_call']] = call
                #
                file_name = program['file']
                file_ptr[done - ignored] = file_name
            except (TooLongPathError, InvalidSketchError) as e:
                ignored += 1
            done += 1
            if done % 100000 == 0:
                print('Extracted data for {} programs'.format(done), end='\n')


        print('{:8d} programs/asts in training data'.format(done))
        print('{:8d} programs/asts ignored by given config'.format(ignored))
        print('{:8d} programs/asts to search over'.format(done - ignored))
        print('{:8d} data points total'.format(len(data_points)))

        # randomly shuffle to avoid bias towards initial data points during training
        random.shuffle(data_points)
        evidences, targets, js_programs = zip(*data_points) #unzip

        # save callmap if save location is given
        if not infer:
            if save is not None:
                with open(os.path.join(save, 'callmap.pkl'), 'wb') as f:
                    pickle.dump(callmap, f)

            with open(os.path.join(save, 'file_ptr.pkl'), 'wb') as f:
                pickle.dump(file_ptr, f)

        return evidences, targets, js_programs
