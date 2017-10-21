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

import argparse
import os
import json
import collections

from bayou.core.model import Model
from bayou.core.utils import CHILD_EDGE, SIBLING_EDGE
from bayou.core.utils import read_config

MAX_GEN_UNTIL_STOP = 99
MAX_AST_DEPTH = 99


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
        self.evidences = evidences
        self.calls_in_last_ast = []
        return self.generate_ast(psi)

    def psi_random(self):
        return np.random.normal(size=[1, self.model.config.latent_size])

    def psi_from_evidence(self, js_evidences):
        return self.model.infer_psi(self.sess, js_evidences)

    def gen_until_STOP(self, psi, depth, in_nodes, in_edges):
        if len(in_nodes) != len(in_edges):
            raise ValueError('Nodes: {}, Edges: {}'.format(str(in_nodes), str(in_edges)))
        ast = []
        nodes, edges = in_nodes[:], in_edges[:]
        num = 0
        while True:
            assert num < MAX_GEN_UNTIL_STOP  # exception caught in main
            prediction = self.get_prediction(psi, nodes, edges)
            if prediction == 'STOP':
                break
            nodes += [prediction]
            js = self.generate_ast(psi, depth+1, nodes, edges)
            ast.append(js)
            edges += [SIBLING_EDGE]
            num += 1
        return ast

    def get_prediction(self, psi, nodes, edges):
        if len(nodes) != len(edges):
            raise ValueError('Nodes: {}, Edges: {}'.format(str(nodes), str(edges)))
        dist = self.model.infer_ast(self.sess, self.evidences, psi, nodes, edges)
        idx = np.random.choice(range(len(dist)), p=dist)
        prediction = self.model.config.decoder.chars[idx]
        return prediction

    def generate_ast(self, psi, depth=0, in_nodes=['DOMMethodDeclaration'], in_edges=[]):
        if len(in_nodes) != len(in_edges) + 1:
            raise ValueError('Nodes: {}, Edges: {}'.format(str(in_nodes), str(in_edges)))
        assert depth < MAX_AST_DEPTH
        ast = collections.OrderedDict()
        node_type = in_nodes[-1]
        ast['node'] = node_type
        nodes, edges = in_nodes[:], in_edges[:]

        edges += [CHILD_EDGE]
        if node_type == 'DOMAssignment':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_lhs'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_rhs'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        if node_type == 'DOMBlock':
            ast['_statements'] = self.gen_until_STOP(psi, depth, nodes, edges)
            return ast

        if node_type == 'DOMCatchClause':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_type'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_variable'] = prediction
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_body'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        if node_type == 'DOMClassInstanceCreation':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_type'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            ast['_arguments'] = self.gen_until_STOP(psi, depth, nodes, edges)
            return ast

        if node_type == 'DOMExpressionStatement':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_expression'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        if node_type == 'DOMIfStatement':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_cond'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_then'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_else'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        if node_type == 'DOMInfixExpression':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_left'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_operator'] = prediction
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_right'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        if node_type == 'DOMMethodDeclaration':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_body'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        if node_type == 'DOMMethodInvocation':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_expression'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_name'] = prediction
            edges += [SIBLING_EDGE]
            ast['_arguments'] = self.gen_until_STOP(psi, depth, nodes, edges)
            return ast

        if node_type == 'DOMName':
            prediction = self.get_prediction(psi, nodes, edges)
            ast['_name'] = prediction
            return ast

        if node_type == 'DOMNullLiteral':
            return ast

        if node_type == 'DOMNumberLiteral':
            prediction = self.get_prediction(psi, nodes, edges)
            ast['_value'] = prediction
            return ast

        if node_type == 'DOMParenthesizedExpression':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_expression'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        if node_type == 'DOMTryStatement':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_body'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            ast['_clauses'] = self.gen_until_STOP(psi, depth, nodes, edges)
            ast['_finally'] = None  # always
            return ast

        if node_type == 'DOMType':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['type'] = prediction
            edges += [SIBLING_EDGE]
            ast['parameters'] = self.gen_until_STOP(psi, depth, nodes, edges)
            return ast

        if node_type == 'DOMVariableDeclarationFragment':
            prediction = self.get_prediction(psi, nodes, edges)
            ast['_name'] = prediction
            ast['_initializer'] = None  # always
            return ast

        if node_type == 'DOMVariableDeclarationStatement':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_type'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            ast['_fragments'] = self.gen_until_STOP(psi, depth, nodes, edges)
            return ast

        if node_type == 'DOMWhileStatement':
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_cond'] = self.generate_ast(psi, depth+1, nodes, edges)
            edges += [SIBLING_EDGE]
            prediction = self.get_prediction(psi, nodes, edges)
            nodes += [prediction]
            ast['_body'] = self.generate_ast(psi, depth+1, nodes, edges)
            return ast

        assert False  # no need to crash, just discard this AST
