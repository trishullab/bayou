import tensorflow as tf

import argparse
import os
import sys
import json
import pickle
import collections

sys.path.append(os.path.abspath(os.path.join('..', '')))
from dsl import *

from model import Model
from utils import weighted_pick

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, default='save',
                       help='model directory to laod from')
    parser.add_argument('input_file', type=str, nargs=1,
                       help='input file containing set of sequences (in JSON)')
    parser.add_argument('--output_file', type=str, default=None,
                       help='file to print AST (in JSON) to')
    parser.add_argument('--n', type=int, default=1,
                       help='number of ASTs to sample/synthesize')

    args = parser.parse_args()
    with tf.Session() as sess:
        predictor = Predictor(args, sess)
        c, err = 0, 0
        asts = []
        while c < args.n:
            try:
                ast, p_ast = predictor.generate_ast()
                ast['p_ast'] = p_ast
                asts.append(ast)
                c += 1
            except KeyError:
                err += 1

    if args.output_file is None:
        print(json.dumps(asts, indent=2))
    else:
        with open(args.output_file, 'w') as f:
            json.dump(asts, fp=f, indent=2)
    print('Number of errors: {}'.format(err))

class Predictor(object):

    def __init__(self, args, sess):
        # parse the input sequences
        with open(args.input_file[0]) as f:
            self.seqs = json.load(f)
        self.sess = sess

        # load the saved vocabularies
        with open(os.path.join(args.save_dir, 'config.pkl'), 'rb') as f:
            saved_args = pickle.load(f)
        with open(os.path.join(args.save_dir, 'chars_vocab.pkl'), 'rb') as f:
            _, self.input_vocab, self.target_chars, self.target_vocab = pickle.load(f)
        self.model = Model(saved_args, True)

        # restore the saved model
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(args.save_dir)
        assert ckpt and ckpt.model_checkpoint_path, 'Malformed model files'
        saver.restore(self.sess, ckpt.model_checkpoint_path)

    def generate_ast(self, in_nodes=['DBlock'], in_edges=[CHILD_EDGE]):
        ast = collections.OrderedDict()
        node = in_nodes[-1]
        ast['node'] = node
        p_ast = 1. # probability of generating this AST
        nodes, edges = in_nodes[:], in_edges[:]
        # print(list(zip(nodes, edges)))
        for child in ast_map[node]:
            if type(child) == list:
                child, _ = child[0] # assuming that all list types in DSL are non-terminals
                ast_child = []
                while True:
                    dist = self.model.infer(self.sess, self.seqs, nodes, edges, self.input_vocab,
                                                self.target_vocab)
                    idx = weighted_pick(dist)
                    p_ast *= dist[idx]
                    prediction = self.target_chars[idx]
                    if prediction == STOP[0] or prediction == None:
                        break
                    nodes += [prediction]
                    js, p = self.generate_ast(nodes, edges + [CHILD_EDGE])
                    js['p'] = float(dist[idx])
                    ast_child.append(js)
                    p_ast *= p
                    edges += [SIBLING_EDGE]
                ast[child] = ast_child
            else:
                dist = self.model.infer(self.sess, self.seqs, nodes, edges, self.input_vocab,
                                            self.target_vocab)
                idx = weighted_pick(dist)
                prediction = self.target_chars[idx]
                nodes += [prediction]
                c, nt = child
                if nt and prediction is not None:
                    js, p = self.generate_ast(nodes, edges + [CHILD_EDGE])
                    js['p'] = float(dist[idx])
                else:
                    js, p = prediction, 1. # dist[idx] is multiplied with below
                    ast['p_{0}'.format(c)] = float(dist[idx])
                ast[c] = js
                p_ast *= dist[idx] * p
                edges += [SIBLING_EDGE]
        return ast, float(p_ast)

if __name__ == '__main__':
    main()
