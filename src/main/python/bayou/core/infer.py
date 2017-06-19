from __future__ import print_function
import tensorflow as tf
import numpy as np

import argparse
import os
import json
import collections
import logging

from bayou.core.model import Model
from bayou.core.utils import CHILD_EDGE, SIBLING_EDGE
from bayou.core.utils import read_config

MAX_GEN_UNTIL_STOP = 20
MAX_AST_DEPTH = 10


def infer(clargs):
    with tf.Session() as sess:
        predictor = BayesianPredictor(clargs.save, sess)
        err = 0
        asts = []
        if clargs.evidence_file:
            with open(clargs.evidence_file) as f:
                js = json.load(f)
        for c in range(clargs.n):
            print('Generated {} ASTs ({} errors)'.format(c, err), end='\r')
            try:
                if clargs.random:
                    psi = predictor.psi_random()
                else:
                    psi = predictor.psi_from_evidence(js)
                ast = predictor.generate_ast(psi)
                if clargs.plot2d:
                    ast['psi'] = list(psi[0])
                asts.append(ast)
                c += 1
            except AssertionError:
                err += 1
    if clargs.plot2d:
        if predictor.model.config.latent_size == 2:
            plot2d(asts)
        else:
            print('Latent space is not 2-dimensional.. cannot plot')

    if clargs.output_file is None:
        print(json.dumps({'asts': asts}, indent=2))
    else:
        with open(clargs.output_file, 'w') as f:
            json.dump({'asts': asts}, fp=f, indent=2)
    print('Number of errors: {}'.format(err))


class BayesianPredictor(object):
    def __init__(self, save, sess):
        self.sess = sess

        # load the saved config
        with open(os.path.join(save, 'config.json')) as f:
            config = read_config(json.load(f), save_dir=save, infer=True)
        self.model = Model(config, True)

        # restore the saved model
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(save)
        saver.restore(self.sess, ckpt.model_checkpoint_path)

    def psi_random(self):
        return np.random.normal(size=[1, self.model.config.latent_size])

    def psi_from_evidence(self, js_evidences):
        logging.debug("entering")
        psi = self.model.infer_psi(self.sess, js_evidences)
        logging.debug("exiting")
        return psi

    def gen_until_STOP(self, psi, in_nodes, in_edges, depth, check_call=False):
        ast = []
        nodes, edges = in_nodes[:], in_edges[:]
        num = 0
        while True:
            assert num < MAX_GEN_UNTIL_STOP  # exception caught in main
            dist = self.model.infer_ast(self.sess, psi, nodes, edges)
            idx = np.random.choice(range(len(dist)), p=dist)
            prediction = self.model.config.decoder.chars[idx]
            nodes += [prediction]
            if check_call:  # exception caught in main
                assert prediction not in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']
            if prediction == 'STOP':
                edges += [SIBLING_EDGE]
                break
            js = self.generate_ast(psi, nodes, edges + [CHILD_EDGE], depth + 1)
            ast.append(js)
            edges += [SIBLING_EDGE]
            num += 1
        return ast, nodes, edges

    def generate_ast(self, psi, in_nodes=['DSubTree'], in_edges=[CHILD_EDGE], depth=0):
        logging.debug("entering")
        ast = collections.OrderedDict()
        node = in_nodes[-1]
        assert depth < MAX_AST_DEPTH

        # Return the "AST" if the node is an API call
        if node not in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']:
            ast['node'] = 'DAPICall'
            ast['_call'] = node
            logging.debug("exiting")
            return ast

        ast['node'] = node
        nodes, edges = in_nodes[:], in_edges[:]

        if node == 'DBranch':
            ast_cond, nodes, edges = self.gen_until_STOP(psi, nodes, edges, depth, check_call=True)
            ast_then, nodes, edges = self.gen_until_STOP(psi, nodes, edges, depth)
            ast_else, nodes, edges = self.gen_until_STOP(psi, nodes, edges, depth)
            ast['_cond'] = ast_cond
            ast['_then'] = ast_then
            ast['_else'] = ast_else
            logging.debug("exiting")
            return ast

        if node == 'DExcept':
            ast_try, nodes, edges = self.gen_until_STOP(psi, nodes, edges, depth)
            ast_catch, nodes, edges = self.gen_until_STOP(psi, nodes, edges, depth)
            ast['_try'] = ast_try
            ast['_catch'] = ast_catch
            logging.debug("exiting")
            return ast

        if node == 'DLoop':
            ast_cond, nodes, edges = self.gen_until_STOP(psi, nodes, edges, depth, check_call=True)
            ast_body, nodes, edges = self.gen_until_STOP(psi, nodes, edges, depth)
            ast['_cond'] = ast_cond
            ast['_body'] = ast_body
            logging.debug("exiting")
            return ast

        if node == 'DSubTree':
            ast_nodes, _, _ = self.gen_until_STOP(psi, nodes, edges, depth)
            ast['_nodes'] = ast_nodes
            logging.debug("exiting")
            return ast

        logging.debug("exiting")


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
        sample = ast['psi']
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
    parser = argparse.ArgumentParser()
    parser.add_argument('--save', type=str, default='save',
                        help='directory to laod model from')
    parser.add_argument('--evidence_file', type=str, default=None,
                        help='input file containing evidences (in JSON)')
    parser.add_argument('--random', action='store_true',
                        help='print random ASTs by sampling from Normal(0,1) (ignores evidences)')
    parser.add_argument('--plot2d', action='store_true',
                        help='plots the (2d) sampled psi values in scatterplot (requires --random)')
    parser.add_argument('--output_file', type=str, default=None,
                        help='file to print AST (in JSON) to')
    parser.add_argument('--n', type=int, default=1,
                        help='number of ASTs to sample/synthesize')

    clargs = parser.parse_args()
    if not clargs.evidence_file and not clargs.random:
        parser.error('Provide at least one option: --evidence_file or --random')
    if clargs.plot2d and not clargs.random:
        parser.error('--plot2d requires --random (otherwise there is only one psi to plot)')
    infer(clargs)
