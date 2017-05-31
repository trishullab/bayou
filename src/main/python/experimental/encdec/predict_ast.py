import tensorflow as tf

import argparse
import os
import sys
import json
import pickle
import random
import collections

from experimental.encdec.model import Model
from experimental.encdec.utils import weighted_pick
from experimental.encdec.data_reader import sub_sequences, CHILD_EDGE, SIBLING_EDGE

MAX_GEN_UNTIL_STOP = 20

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--seqs_file', required=True, type=str,
                       help='input file containing set of sequences (in JSON)')
    parser.add_argument('--save_dir', type=str, default='save',
                       help='model directory to laod from')
    parser.add_argument('--output_file', type=str, default=None,
                       help='file to print AST (in JSON) to')
    parser.add_argument('--n', type=int, default=1,
                       help='number of ASTs to sample/synthesize')

    args = parser.parse_args()
    with tf.Session() as sess:
        predictor = Predictor(args.save_dir, sess)
        c, err = 0, 0
        asts = []
        with open(args.seqs_file) as f:
            seqs = json.load(f)
        seqs = sub_sequences(seqs, predictor.model.args)
        while c < args.n:
            print('generated {} ASTs ({} errors)'.format(c, err), end='\r')
            try:
                ast, p_ast = predictor.generate_ast(seqs)
                ast['p_ast'] = p_ast
                asts.append(ast)
                c += 1
            except AssertionError:
                err += 1

    if args.output_file is None:
        print(json.dumps({ 'asts': asts }, indent=2))
    else:
        with open(args.output_file, 'w') as f:
            json.dump({ 'asts': asts }, fp=f, indent=2)
    print('Number of errors: {}'.format(err))

class Predictor(object):

    def __init__(self, save_dir, sess):
        self.sess = sess

        # load the saved vocabularies
        with open(os.path.join(save_dir, 'config.pkl'), 'rb') as f:
            saved_args = pickle.load(f)
        with open(os.path.join(save_dir, 'chars_vocab.pkl'), 'rb') as f:
            _, self.input_vocab, self.target_chars, self.target_vocab = pickle.load(f)
        self.model = Model(saved_args, True)

        # restore the saved model
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(save_dir)
        assert ckpt and ckpt.model_checkpoint_path, 'Malformed model files'
        saver.restore(self.sess, ckpt.model_checkpoint_path)

    def gen_until_STOP(self, seqs, in_nodes, in_edges, check_call=False):
        ast = []
        p_ast = 1. # probability of generating this AST
        nodes, edges = in_nodes[:], in_edges[:]
        num = 0
        while True:
            assert num < MAX_GEN_UNTIL_STOP # exception caught in main
            dist = self.model.infer(self.sess, seqs, nodes, edges, self.input_vocab,
                                    self.target_vocab)
            idx = weighted_pick(dist)
            p_ast *= dist[idx]
            prediction = self.target_chars[idx]
            nodes += [prediction]
            if check_call: # exception caught in main
                assert prediction not in [ 'DBranch', 'DExcept', 'DLoop', 'DSubTree' ]
            if prediction == 'STOP':
                edges += [SIBLING_EDGE]
                break
            js, p = self.generate_ast(seqs, nodes, edges + [CHILD_EDGE])
            js['p'] = float(dist[idx])
            ast.append(js)
            p_ast *= p
            edges += [SIBLING_EDGE]
            num += 1
        return ast, p_ast, nodes, edges

    def generate_ast(self, seqs, in_nodes=['DSubTree'], in_edges=[CHILD_EDGE]):
        ast = collections.OrderedDict()
        node = in_nodes[-1]

        # Return the "AST" if the node is an API call
        if node not in [ 'DBranch', 'DExcept', 'DLoop', 'DSubTree' ]:
            ast['node'] = 'DAPICall'
            ast['_call'] = node
            return ast, 1.

        ast['node'] = node
        nodes, edges = in_nodes[:], in_edges[:]

        if node == 'DBranch':
            ast_cond, pC, nodes, edges = self.gen_until_STOP(seqs, nodes, edges, check_call=True)
            ast_then, p1, nodes, edges = self.gen_until_STOP(seqs, nodes, edges)
            ast_else, p2, nodes, edges = self.gen_until_STOP(seqs, nodes, edges)
            ast['_cond'] = ast_cond
            ast['_then'] = ast_then
            ast['_else'] = ast_else
            p_ast = pC * p1 * p2
            return ast, float(p_ast)

        if node == 'DExcept':
            ast_try, p1, nodes, edges = self.gen_until_STOP(seqs, nodes, edges)
            ast_catch, p2, nodes, edges = self.gen_until_STOP(seqs, nodes, edges)
            ast['_try'] = ast_try
            ast['_catch'] = ast_catch
            p_ast = p1 * p2
            return ast, float(p_ast)

        if node == 'DLoop':
            ast_cond, pC, nodes, edges = self.gen_until_STOP(seqs, nodes, edges, check_call=True)
            ast_body, p1, nodes, edges = self.gen_until_STOP(seqs, nodes, edges)
            ast['_cond'] = ast_cond
            ast['_body'] = ast_body
            p_ast = pC * p1
            return ast, float(p_ast)

        if node == 'DSubTree':
            ast_nodes, p_ast, _, _ = self.gen_until_STOP(seqs, nodes, edges)
            ast['_nodes'] = ast_nodes
            return ast, float(p_ast)
        
if __name__ == '__main__':
    main()
