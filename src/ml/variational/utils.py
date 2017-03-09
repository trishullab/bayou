import os
import pickle
import itertools
import numpy as np

from variational.data_reader import read_data, CHILD_EDGE

CLASS0 = 'CLASS0'

def weighted_pick(weights):
    t = np.cumsum(weights)
    s = np.sum(weights)
    return int(np.searchsorted(t, np.random.rand(1)*s))

class DataLoader():
    def __init__(self, input_file, args):
        self.args = args

        # read the raw inputs and targets
        print("reading text file")
        raw_inputs_seqs, raw_inputs_kws, raw_targets = read_data(input_file, args)
        assert len(raw_inputs_seqs) == len(raw_targets) and \
               len(raw_inputs_kws) == len(raw_targets), 'Inputs and targets do not align'

        # setup input and target chars/vocab, using class 0 for padding
        if args.init_from is None:
            self.input_chars_seqs = [CLASS0] + list(set([w for p in raw_inputs_seqs for s in p for w in s]))
            self.input_vocab_seqs = dict(zip(self.input_chars_seqs, range(len(self.input_chars_seqs))))

            self.input_chars_kws = [CLASS0] + list(set([w for p in raw_inputs_kws for w in p]))
            self.input_vocab_kws = dict(zip(self.input_chars_kws, range(len(self.input_chars_kws))))

            self.target_chars = [CLASS0] + list(set([node for path in raw_targets for (node, _) in path]))
            self.target_vocab = dict(zip(self.target_chars, range(len(self.target_chars))))
        else:
            with open(os.path.join(args.init_from, 'chars_vocab.pkl'), 'rb') as f:
                self.input_chars_seqs, self.input_vocab_seqs, self.input_chars_kws, self.input_vocab_kws, \
                        self.target_chars, self.target_vocab = pickle.load(f)

        args.input_vocab_seqs_size = len(self.input_vocab_seqs)
        args.input_vocab_kws_size = len(self.input_vocab_kws)
        args.target_vocab_size = len(self.target_vocab)

        # align with number of batches
        args.num_batches = int(len(raw_inputs_seqs) / args.batch_size)
        assert args.num_batches > 0, 'Not enough data'
        sz = args.num_batches * args.batch_size
        raw_inputs_seqs = raw_inputs_seqs[:sz]
        raw_inputs_kws = raw_inputs_kws[:sz]
        raw_targets = raw_targets[:sz]

        # apply the dict on inputs and targets
        self.input_seqs = np.zeros((sz, args.max_seqs, args.max_seq_length, 1), dtype=np.int32)
        for i, set_of_seqs in enumerate(raw_inputs_seqs):
            assert len(set_of_seqs) <= args.max_seqs, 'Too many sequences, increase max_seqs'
            for j, seq in enumerate(set_of_seqs):
                assert len(seq) <= args.max_seq_length, 'Sequence too long, increase max_seq_length'
                self.input_seqs[i, j, :len(seq), 0] = list(map(self.input_vocab_seqs.get, seq))

        self.input_kws = np.zeros((sz, args.max_keywords, 1, 1), dtype=np.int32)
        for i, kws in enumerate(raw_inputs_kws):
            assert len(kws) <= args.max_keywords, 'Too many keywords, increase max_keywords'
            self.input_kws[i, :len(kws), 0, 0] = list(map(self.input_vocab_kws.get, kws))

        self.nodes = np.zeros((sz, args.max_ast_depth), dtype=np.int32)
        self.edges = np.zeros((sz, args.max_ast_depth), dtype=np.bool)
        self.targets = np.zeros((sz, args.max_ast_depth), dtype=np.int32)
        for i, path in enumerate(raw_targets):
            assert len(path) <= args.max_ast_depth, 'Path too long, increase max_ast_depth'
            self.nodes[i, :len(path)] = list(map(self.target_vocab.get, [p[0] for p in path]))
            self.edges[i, :len(path)] = [p[1] == CHILD_EDGE for p in path]
            self.targets[i, :len(path)-1] = self.nodes[i, 1:len(path)] # shifted left by one

        # split into batches
        self.input_seqs = np.split(self.input_seqs, args.num_batches, axis=0)
        self.input_kws = np.split(self.input_kws, args.num_batches, axis=0)
        self.nodes = np.split(self.nodes, args.num_batches, axis=0)
        self.edges = np.split(self.edges, args.num_batches, axis=0)
        self.targets = np.split(self.targets, args.num_batches, axis=0)

        # reset batches
        self.reset_batches()

    def next_batch(self):
        x, k, n, e, y = next(self.batches)

        # reshape the batch into required format
        rx = [x[:, i, :, :] for i in range(self.args.max_seqs)]
        rk = [k[:, i, :, :] for i in range(self.args.max_keywords)]
        rn = np.transpose(n)
        re = np.transpose(e)

        return rx, rk, rn, re, y

    def reset_batches(self):
        self.batches = zip(self.input_seqs, self.input_kws, self.nodes, self.edges, self.targets)
