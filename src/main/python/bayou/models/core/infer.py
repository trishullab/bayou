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
import tensorflow as tf
import numpy as np

import os
import json
import collections

from bayou.models.core.model import Model
from bayou.models.core.utils import CHILD_EDGE, SIBLING_EDGE
from bayou.models.core.utils import read_config

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
        self.calls_in_last_ast = []
        return self.generate_ast(psi)

    def psi_random(self):
        return np.random.normal(size=[1, self.model.config.latent_size])

    def psi_from_evidence(self, js_evidences):
        return self.model.infer_psi(self.sess, js_evidences)

    def gen_until_STOP(self, psi, depth, in_nodes, in_edges, check_call=False):
        ast = []
        nodes, edges = in_nodes[:], in_edges[:]
        num = 0
        while True:
            assert num < MAX_GEN_UNTIL_STOP # exception caught in main
            dist = self.model.infer_ast(self.sess, psi, nodes, edges)
            idx = np.random.choice(range(len(dist)), p=dist)
            prediction = self.model.config.decoder.chars[idx]
            nodes += [prediction]
            if check_call:  # exception caught in main
                assert prediction not in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']
            if prediction == 'STOP':
                edges += [SIBLING_EDGE]
                break
            js = self.generate_ast(psi, depth + 1, nodes, edges + [CHILD_EDGE])
            ast.append(js)
            edges += [SIBLING_EDGE]
            num += 1
        return ast, nodes, edges

    def generate_ast(self, psi, depth=0, in_nodes=['DSubTree'], in_edges=[CHILD_EDGE]):
        assert depth < MAX_AST_DEPTH
        ast = collections.OrderedDict()
        node = in_nodes[-1]

        # Return the "AST" if the node is an API call
        if node not in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']:
            ast['node'] = 'DAPICall'
            ast['_call'] = node
            self.calls_in_last_ast.append(node)
            return ast

        ast['node'] = node
        nodes, edges = in_nodes[:], in_edges[:]

        if node == 'DBranch':
            ast_cond, nodes, edges = self.gen_until_STOP(psi, depth, nodes, edges, check_call=True)
            ast_then, nodes, edges = self.gen_until_STOP(psi, depth, nodes, edges)
            ast_else, nodes, edges = self.gen_until_STOP(psi, depth, nodes, edges)
            ast['_cond'] = ast_cond
            ast['_then'] = ast_then
            ast['_else'] = ast_else
            return ast

        if node == 'DExcept':
            ast_try, nodes, edges = self.gen_until_STOP(psi, depth, nodes, edges)
            ast_catch, nodes, edges = self.gen_until_STOP(psi, depth, nodes, edges)
            ast['_try'] = ast_try
            ast['_catch'] = ast_catch
            return ast

        if node == 'DLoop':
            ast_cond, nodes, edges = self.gen_until_STOP(psi, depth, nodes, edges, check_call=True)
            ast_body, nodes, edges = self.gen_until_STOP(psi, depth, nodes, edges)
            ast['_cond'] = ast_cond
            ast['_body'] = ast_body
            return ast

        if node == 'DSubTree':
            ast_nodes, _, _ = self.gen_until_STOP(psi, depth, nodes, edges)
            ast['_nodes'] = ast_nodes
            return ast
