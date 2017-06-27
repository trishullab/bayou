from __future__ import print_function
import json
import numpy as np
import random
from collections import Counter

from bayou.experiments.low_level_sketches.utils import C0


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
            counts = Counter([token for tokens in raw_targets for token in tokens])
            counts[C0] = 1
            config.decoder.chars = sorted(counts.keys(), key=lambda w: counts[w], reverse=True)
            config.decoder.vocab = dict(zip(config.decoder.chars, range(len(config.decoder.chars))))
            config.decoder.vocab_size = len(config.decoder.vocab)

        # wrangle the evidences and targets into numpy arrays
        self.inputs = [ev.wrangle(data) for ev, data in zip(config.evidence, raw_evidences)]
        self.tokens = np.zeros((sz, config.decoder.max_tokens), dtype=np.int32)
        self.targets = np.zeros((sz, config.decoder.max_tokens), dtype=np.int32)
        for i, tokens in enumerate(raw_targets):
            self.tokens[i, :len(tokens)] = list(map(config.decoder.vocab.get, tokens))
            self.targets[i, :len(tokens)-1] = self.tokens[i, 1:len(tokens)]  # shifted left by one

        # split into batches
        self.inputs = [np.split(ev_data, config.num_batches, axis=0) for ev_data in self.inputs]
        self.tokens = np.split(self.tokens, config.num_batches, axis=0)
        self.targets = np.split(self.targets, config.num_batches, axis=0)

        # reset batches
        self.reset_batches()

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
                tokens = program['low_level_sketch'].split()
                assert len(tokens) <= self.config.decoder.max_tokens
                evidences.append(evidence)
                targets.append(tokens)
            except AssertionError:
                ignored += 1
            done += 1
            print('{:8d} programs in training data'.format(done), end='\r')
        print('\n{:8d} programs ignored by given config'.format(ignored))

        # randomly shuffle to avoid bias towards initial data points during training
        data_points = list(zip(evidences, targets))
        random.shuffle(data_points)
        evidences, targets = zip(*data_points)

        return evidences, targets

    def next_batch(self):
        batch = next(self.batches)
        t, y = batch[:2]
        ev_data = batch[2:]

        # reshape the batch into required format
        rt = np.transpose(t)

        return ev_data, rt, y

    def reset_batches(self):
        self.batches = iter(zip(self.tokens, self.targets, *self.inputs))
