import tensorflow as tf
import numpy as np

import argparse
import os
import sys
import json
import pickle
import collections

from model import Model
from utils import weighted_pick
from data_reader import CHILD_EDGE, SIBLING_EDGE

MAX_GEN_UNTIL_STOP = 20

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, default='save',
                       help='model directory to laod from')
    parser.add_argument('--seqs_file', type=str, default=None,
                       help='input file containing set of sequences (in JSON)')
    parser.add_argument('--random', action='store_true',
                       help='print random ASTs by sampling from Normal(0,1) (ignores sequences)')
    parser.add_argument('--plot2d', action='store_true',
                       help='(requires --random) plots the (2d) sampled psi values in scatterplot')
    parser.add_argument('--output_file', type=str, default=None,
                       help='file to print AST (in JSON) to')
    parser.add_argument('--n', type=int, default=1,
                       help='number of ASTs to sample/synthesize')

    args = parser.parse_args()
    if args.seqs_file is None and not args.random:
        parser.error('At least one of --seqs_file or --random is required')
    if args.plot2d and not args.random:
        parser.error('--plot2d requires --random (otherwise there is only one psi to plot)')
    with tf.Session() as sess:
        predictor = Predictor(args, sess)
        c, err = 0, 0
        asts = []
        while c < args.n:
            print('generated {} ASTs ({} errors)'.format(c, err), end='\r')
            try:
                if args.random:
                    psi = np.random.normal(size=[1, predictor.model.args.latent_size])
                    ast, p_ast = predictor.generate_ast(psi=psi)
                    ast['psi_sample'] = list(psi[0])
                else:
                    ast, p_ast = predictor.generate_ast()
                ast['p_ast'] = p_ast
                asts.append(ast)
                c += 1
            except AssertionError:
                err += 1
    if args.plot2d:
        if predictor.model.args.latent_size == 2:
            plot2d(asts)
        else:
            print('Latent space is not 2-dimensional.. cannot plot')

    if args.output_file is None:
        print(json.dumps({ 'asts': asts }, indent=2))
    else:
        with open(args.output_file, 'w') as f:
            json.dump({ 'asts': asts }, fp=f, indent=2)
    print('Number of errors: {}'.format(err))

class Predictor(object):

    def __init__(self, args, sess):
        # parse the input sequences
        if args.random:
            self.seqs = []
        else:
            with open(args.seqs_file) as f:
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

    def gen_until_STOP(self, in_nodes, in_edges, check_call=False, psi=None):
        ast = []
        p_ast = 1. # probability of generating this AST
        nodes, edges = in_nodes[:], in_edges[:]
        num = 0
        while True:
            assert num < MAX_GEN_UNTIL_STOP # exception caught in main
            dist = self.model.infer(self.sess, self.seqs, nodes, edges, self.input_vocab,
                                    self.target_vocab, psi)
            idx = weighted_pick(dist)
            p_ast *= dist[idx]
            prediction = self.target_chars[idx]
            nodes += [prediction]
            if check_call: # exception caught in main
                assert prediction not in [ 'DBranch', 'DExcept', 'DLoop', 'DSubTree' ]
            if prediction == 'STOP':
                edges += [SIBLING_EDGE]
                break
            js, p = self.generate_ast(nodes, edges + [CHILD_EDGE])
            js['p'] = float(dist[idx])
            ast.append(js)
            p_ast *= p
            edges += [SIBLING_EDGE]
            num += 1
        return ast, p_ast, nodes, edges

    def generate_ast(self, in_nodes=['DSubTree'], in_edges=[CHILD_EDGE], psi=None):
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
            ast_cond, pC, nodes, edges = self.gen_until_STOP(nodes, edges, check_call=True, psi=psi)
            ast_then, p1, nodes, edges = self.gen_until_STOP(nodes, edges, psi=psi)
            ast_else, p2, nodes, edges = self.gen_until_STOP(nodes, edges, psi=psi)
            ast['_cond'] = ast_cond
            ast['_then'] = ast_then
            ast['_else'] = ast_else
            p_ast = pC * p1 * p2
            return ast, float(p_ast)

        if node == 'DExcept':
            ast_try, p1, nodes, edges = self.gen_until_STOP(nodes, edges, psi=psi)
            ast_catch, p2, nodes, edges = self.gen_until_STOP(nodes, edges, psi=psi)
            ast['_try'] = ast_try
            ast['_catch'] = ast_catch
            p_ast = p1 * p2
            return ast, float(p_ast)

        if node == 'DLoop':
            ast_cond, pC, nodes, edges = self.gen_until_STOP(nodes, edges, check_call=True, psi=psi)
            ast_body, p1, nodes, edges = self.gen_until_STOP(nodes, edges, psi=psi)
            ast['_cond'] = ast_cond
            ast['_body'] = ast_body
            p_ast = pC * p1
            return ast, float(p_ast)

        if node == 'DSubTree':
            ast_nodes, p_ast, _, _ = self.gen_until_STOP(nodes, edges, psi=psi)
            ast['_nodes'] = ast_nodes
            return ast, float(p_ast)

def find_api(nodes):
    for node in nodes:
        if node['node'] == 'DAPICall':
            call = node['_call'].split('.')
            api = '.'.join(call[:3])
            return api
    return None

def plot2d(asts):
    import matplotlib.pyplot as plt
    import matplotlib.cm as cm
    dic = {}
    for ast in asts:
        sample = ast['psi_sample']
        api = find_api(ast['_nodes'])
        if api is None:
            continue
        if api not in dic:
            dic[api] = []
        dic[api].append(sample)

    apis = dic.keys()
    colors = cm.rainbow(np.linspace(0, 1, len(dic)))
    plotpoints = []
    for api, color in zip(apis, colors):
        x = list(map(lambda s: s[0], dic[api]))
        y = list(map(lambda s: s[1], dic[api]))
        plotpoints.append(plt.scatter(x, y, color=color))

    plt.legend(plotpoints, apis, scatterpoints=1, loc='lower left', ncol=3, fontsize=8)
    plt.axhline(0, color='black')
    plt.axvline(0, color='black')
    plt.show()
        
if __name__ == '__main__':
    main()
