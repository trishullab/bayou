import os
import pickle
import itertools
import numpy as np

from encdec.data_reader import read_data, CHILD_EDGE

CLASS0 = 'CLASS0'

def weighted_pick(weights):
    t = np.cumsum(weights)
    s = np.sum(weights)
    return int(np.searchsorted(t, np.random.rand(1)*s))

class DataLoader():
    def __init__(self, input_file, args):
        self.args = args

        # each input is a (sorted) set of sequences, each output is ONE path in the AST
        print("reading text file")
        raw_inputs, raw_targets = read_data(input_file, args)
        assert len(raw_inputs) == len(raw_targets), 'Number of inputs and targets do not match'

        # setup input and target chars/vocab, using class 0 for padding
        if args.init_from is None:
            self.input_chars = [CLASS0] + list(set([w for p in raw_inputs for s in p for w in s]))
            self.input_vocab = dict(zip(self.input_chars, range(len(self.input_chars))))

            self.target_chars = [CLASS0] + list(set([node for path in raw_targets 
                                                          for (node, _) in path]))
            self.target_vocab = dict(zip(self.target_chars, range(len(self.target_chars))))
        else:
            with open(os.path.join(args.init_from, 'chars_vocab.pkl'), 'rb') as f:
                self.input_chars, self.input_vocab, self.target_chars, self.target_vocab = \
                        pickle.load(f)

        args.input_vocab_size = len(self.input_vocab)
        args.target_vocab_size = len(self.target_vocab)

        # align with number of batches
        args.num_batches = int(len(raw_inputs) / args.batch_size)
        assert args.num_batches > 0, 'Not enough data'
        sz = args.num_batches * args.batch_size
        raw_inputs = raw_inputs[:sz]
        raw_targets = raw_targets[:sz]

        # apply the dict on inputs and targets
        self.inputs = np.zeros((sz, args.max_seqs, args.max_seq_length, 1), dtype=np.int32)
        self.inputs_len = np.zeros((sz, args.max_seqs), dtype=np.int32)
        for i, set_of_seqs in enumerate(raw_inputs):
            assert len(set_of_seqs) <= args.max_seqs, 'Too many sequences, increase max_seqs'
            for j, seq in enumerate(set_of_seqs):
                assert len(seq) <= args.max_seq_length, 'Sequence too long, increase max_seq_length'
                self.inputs[i, j, :len(seq), 0] = list(map(self.input_vocab.get, seq))
                self.inputs_len[i, j] = len(seq)

        self.nodes = np.zeros((sz, args.max_ast_depth), dtype=np.int32)
        self.edges = np.zeros((sz, args.max_ast_depth), dtype=np.bool)
        self.targets = np.zeros((sz, args.max_ast_depth), dtype=np.int32)
        for i, path in enumerate(raw_targets):
            assert len(path) <= args.max_ast_depth, 'Path too long, increase max_ast_depth'
            self.nodes[i, :len(path)] = list(map(self.target_vocab.get, [p[0] for p in path]))
            self.edges[i, :len(path)] = [p[1] == CHILD_EDGE for p in path]
            self.targets[i, :len(path)-1] = self.nodes[i, 1:len(path)] # shifted left by one

        # split into batches
        self.inputs = np.split(self.inputs, args.num_batches, axis=0)
        self.inputs_len = np.split(self.inputs_len, args.num_batches, axis=0)
        self.nodes = np.split(self.nodes, args.num_batches, axis=0)
        self.edges = np.split(self.edges, args.num_batches, axis=0)
        self.targets = np.split(self.targets, args.num_batches, axis=0)

        # reset batches
        self.reset_batches()

    def next_batch(self):
        x, l, n, e, y = next(self.batches)

        # reshape the batch into required format
        rx = [x[:, i, :, :] for i in range(self.args.max_seqs)]
        rl = [l[:, i] for i in range(self.args.max_seqs)]
        rn = np.transpose(n)
        re = np.transpose(e)

        return rx, rl, rn, re, y

    def reset_batches(self):
        self.batches = zip(self.inputs, self.inputs_len, self.nodes, self.edges, self.targets)
