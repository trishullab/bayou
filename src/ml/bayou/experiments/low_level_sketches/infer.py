from __future__ import print_function
import tensorflow as tf
import numpy as np

import argparse
import os
import json
import collections

from bayou.experiments.low_level_sketches.model import Model
from bayou.experiments.low_level_sketches.utils import read_config

MAX_GEN_UNTIL_STOP = 20
MAX_AST_DEPTH = 5


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

    def infer(self, evidences):
        psi = self.psi_from_evidence(evidences)
        return self.generate_ast(psi)

    def psi_random(self):
        return np.random.normal(size=[1, self.model.config.latent_size])

    def psi_from_evidence(self, js_evidences):
        return self.model.infer_psi(self.sess, js_evidences)

    def gen_until_STOP(self, psi, depth, in_tokens, check_call=False):
        ast = []
        tokens = in_tokens[:]
        num = 0
        while True:
            assert num < MAX_GEN_UNTIL_STOP  # exception caught in main
            dist = self.model.infer_ast(self.sess, psi, tokens)
            idx = np.random.choice(range(len(dist)), p=dist)
            prediction = self.model.config.decoder.chars[idx]
            tokens += [prediction]
            if check_call:  # exception caught in main
                assert prediction not in ['DAPICall', 'DBranch', 'DExcept', 'DLoop', 'DSubTree']
            if prediction == 'STOP':
                break
            js = self.generate_ast(psi, depth + 1, tokens)
            ast.append(js)
            num += 1
        return ast, tokens

    def generate_ast(self, psi, depth=0, in_tokens=[]):
        assert depth < MAX_AST_DEPTH
        ast = collections.OrderedDict()
        token = in_tokens[-1] if in_tokens is not [] else 'DSubTree'

        if token not in ['DAPICall', 'DBranch', 'DExcept', 'DLoop', 'DSubTree']:
            return token

        ast['node'] = token
        tokens = in_tokens[:]

        if token == 'DAPICall':
            ast_call, tokens = self.gen_until_STOP(psi, depth, tokens)
            ast['_call'] = ast_call[0]
            return ast

        if token == 'DBranch':
            ast_cond, tokens = self.gen_until_STOP(psi, depth, tokens, check_call=True)
            ast_then, tokens = self.gen_until_STOP(psi, depth, tokens)
            ast_else, tokens = self.gen_until_STOP(psi, depth, tokens)
            ast['_cond'] = ast_cond
            ast['_then'] = ast_then
            ast['_else'] = ast_else
            return ast

        if token == 'DExcept':
            ast_try, tokens = self.gen_until_STOP(psi, depth, tokens)
            ast_catch, tokens = self.gen_until_STOP(psi, depth, tokens)
            ast['_try'] = ast_try
            ast['_catch'] = ast_catch
            return ast

        if token == 'DLoop':
            ast_cond, tokens = self.gen_until_STOP(psi, depth, tokens, check_call=True)
            ast_body, tokens = self.gen_until_STOP(psi, depth, tokens)
            ast['_cond'] = ast_cond
            ast['_body'] = ast_body
            return ast

        if token == 'DSubTree':
            ast_nodes, _ = self.gen_until_STOP(psi, depth, tokens)
            ast['_nodes'] = ast_nodes
            return ast
