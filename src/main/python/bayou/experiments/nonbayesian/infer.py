from __future__ import print_function

import argparse
import collections
import json
import os

import numpy as np
import tensorflow as tf

from bayou.experiments.nonbayesian.utils import CHILD_EDGE, SIBLING_EDGE
from bayou.experiments.nonbayesian.model import Model
from bayou.experiments.nonbayesian.utils import read_config

MAX_GEN_UNTIL_STOP = 20
MAX_AST_DEPTH = 5


class NonBayesianPredictor(object):

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

    def infer(self, evidences):
        encoding = self.encoding_from_evidence(evidences)
        return self.generate_ast(encoding)

    def encoding_from_evidence(self, js_evidences):
        return self.model.infer_encoding(self.sess, js_evidences)

    def gen_until_STOP(self, encoding, depth, in_nodes, in_edges, check_call=False):
        ast = []
        nodes, edges = in_nodes[:], in_edges[:]
        num = 0
        while True:
            assert num < MAX_GEN_UNTIL_STOP # exception caught in main
            dist = self.model.infer_ast(self.sess, encoding, nodes, edges)
            idx = np.random.choice(range(len(dist)), p=dist)
            prediction = self.model.config.decoder.chars[idx]
            nodes += [prediction]
            if check_call:  # exception caught in main
                assert prediction not in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']
            if prediction == 'STOP':
                edges += [SIBLING_EDGE]
                break
            js = self.generate_ast(encoding, depth + 1, nodes, edges + [CHILD_EDGE])
            ast.append(js)
            edges += [SIBLING_EDGE]
            num += 1
        return ast, nodes, edges

    def generate_ast(self, encoding, depth=0, in_nodes=['DSubTree'], in_edges=[CHILD_EDGE]):
        assert depth < MAX_AST_DEPTH
        ast = collections.OrderedDict()
        node = in_nodes[-1]

        # Return the "AST" if the node is an API call
        if node not in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']:
            ast['node'] = 'DAPICall'
            ast['_call'] = node
            return ast

        ast['node'] = node
        nodes, edges = in_nodes[:], in_edges[:]

        if node == 'DBranch':
            ast_cond, nodes, edges = self.gen_until_STOP(encoding, depth, nodes, edges, check_call=True)
            ast_then, nodes, edges = self.gen_until_STOP(encoding, depth, nodes, edges)
            ast_else, nodes, edges = self.gen_until_STOP(encoding, depth, nodes, edges)
            ast['_cond'] = ast_cond
            ast['_then'] = ast_then
            ast['_else'] = ast_else
            return ast

        if node == 'DExcept':
            ast_try, nodes, edges = self.gen_until_STOP(encoding, depth, nodes, edges)
            ast_catch, nodes, edges = self.gen_until_STOP(encoding, depth, nodes, edges)
            ast['_try'] = ast_try
            ast['_catch'] = ast_catch
            return ast

        if node == 'DLoop':
            ast_cond, nodes, edges = self.gen_until_STOP(encoding, depth, nodes, edges, check_call=True)
            ast_body, nodes, edges = self.gen_until_STOP(encoding, depth, nodes, edges)
            ast['_cond'] = ast_cond
            ast['_body'] = ast_body
            return ast

        if node == 'DSubTree':
            ast_nodes, _, _ = self.gen_until_STOP(encoding, depth, nodes, edges)
            ast['_nodes'] = ast_nodes
            return ast
