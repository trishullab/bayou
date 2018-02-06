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
import pickle
import json

from bayou.models.low_level_evidences.model import Model
from bayou.models.low_level_evidences.utils import CHILD_EDGE, SIBLING_EDGE
from bayou.models.low_level_evidences.utils import read_config

MAX_GEN_UNTIL_STOP = 20
MAX_AST_DEPTH = 5


class TooLongPathError(Exception):
    pass


class IncompletePathError(Exception):
    pass


class InvalidSketchError(Exception):
    pass


class BayesianPredictor(object):

    def __init__(self, save, sess):
        self.sess = sess

        # load the saved config
        with open(os.path.join(save, 'config.json')) as f:
            config = read_config(json.load(f), chars_vocab=True)
        self.model = Model(config, True)

        # load the callmap
        with open(os.path.join(save, 'callmap.pkl'), 'rb') as f:
            self.callmap = pickle.load(f)

        # restore the saved model
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(save)
        saver.restore(self.sess, ckpt.model_checkpoint_path)

    def infer(self, evidences, num_psi_samples=100, beam_width=25):
        """
        Returns an ordered (by probability) list of ASTs from the model, given evidences, using beam search

        :param evidences: the input evidences
        :param num_psi_samples: number of samples of the intent, averaged before AST construction
        :param beam_width: width of the beam search
        :return: list of ASTs ordered by their probabilities
        """
        psis = []
        for i in range(num_psi_samples):
            psis.append(self.psi_from_evidence(evidences))
        psi = np.mean(psis, axis=0)
        return self.generate_asts_beam_search(psi, beam_width)

    def psi_random(self):
        """
        Gets a random intent by sampling from a normal

        :return: random intent
        """
        return np.random.normal(size=[1, self.model.config.latent_size])

    def psi_from_evidence(self, js_evidences):
        """
        Gets a latent intent from the model, given some evidences

        :param js_evidences: the evidences
        :return: the latent intent
        """
        return self.model.infer_psi(self.sess, js_evidences)

    def generate_asts_beam_search(self, psi, beam_width):
        """
        Performs beam search to construct the top-k ASTs

        :param psi: the intent
        :param beam_width: width of beam search (corresponds to the number of results)
        :return: an ordered list of top-k ASTs
        """

        # each candidate is (list of production paths in the AST, likelihood of AST so far)
        candidates = [([[('DSubTree', CHILD_EDGE)]], 1.)]
        complete_candidates = []

        partial_candidate = True
        while partial_candidate:
            partial_candidate = False
            new_candidates = []
            for (candidate, pr) in candidates:

                # gather candidate's complete and incomplete paths
                complete_paths, incomplete_paths = [], []
                try:
                    for path in candidate:
                        if self.is_complete_path(path):
                            complete_paths.append(path)
                        else:
                            incomplete_paths.append(path)
                except (InvalidSketchError, TooLongPathError) as e:
                    continue  # throw out the candidate

                # if candidate is a fully formed AST, add it to new candidates and continue
                if len(incomplete_paths) == 0:
                    complete_candidates.append((candidate, pr))
                    new_candidates.append((candidate, pr))
                    continue
                partial_candidate = True

                # for every incomplete path, create k new candidates from the top k in the next step's dist
                for i, inc_path in enumerate(incomplete_paths):
                    nodes, edges = zip(*inc_path)
                    dist = list(enumerate(self.model.infer_ast(self.sess, psi, nodes, edges)))
                    dist.sort(key=lambda x: x[1], reverse=True)
                    topk = dist[:beam_width]

                    for (idx, p) in topk:
                        new_candidate = [path for path in complete_paths] + \
                                        [path for (j, path) in enumerate(incomplete_paths) if i != j]
                        prediction = self.model.config.decoder.chars[idx]

                        inc_path_step_SIBLING = inc_path + [(prediction, SIBLING_EDGE)]
                        if prediction in ['DBranch', 'DExcept', 'DLoop']:
                            inc_path_step_CHILD = inc_path + [(prediction, CHILD_EDGE)]
                            new_candidates.append((new_candidate + [inc_path_step_CHILD, inc_path_step_SIBLING],
                                                   pr * p * p))
                        else:
                            new_candidates.append((new_candidate + [inc_path_step_SIBLING], pr * p))

            # bound candidates with the beam width
            new_candidates += [c for c in complete_candidates if c not in new_candidates]
            new_candidates.sort(key=lambda x: x[1], reverse=True)
            candidates = new_candidates[:beam_width]

        # convert each set of paths into an AST
        asts = []
        for (candidate, pr) in candidates:
            ast = self.paths_to_ast(candidate)
            ast['probability'] = float(str(pr)[:7])  # "0." + four digits of precision
            if ast not in asts:
                asts.append(ast)
        return asts

    def is_complete_path(self, path):
        """
        Checks if a production path is complete (i.e., no more productions need to be triggered)

        :param path: the path to check for completeness
        :return: boolean indicating if path is complete
        :raise: InvalidSketchError if there is a parse error in the path, TooLongPathError is path is too long
        """
        try:
            if len(path) > 30:
                raise TooLongPathError
            nodes = [node for (node, edge) in path]
            if nodes.count('DBranch') > 2 or nodes.count('DLoop') > 2 or nodes.count('DExcept') > 2:
                raise TooLongPathError
            self.consume_until_STOP(path, 1)
            return True
        except IncompletePathError:
            return False

    def consume_until_STOP(self, path, idx, check_call=False):
        """
        Consumes a path until STOP is encountered, or throw an IncompletePathError. If a DBranch, DExcept or DLoop
        is seen midway when going through the path, recursively attempts to consume the respective node type.

        :param path: the path to consume
        :param idx: current index to start consumption
        :param check_call: if True, check and validate if the nodes in path are call nodes
        :return: the index at which STOP was encountered if there were no recursive consumptions, otherwise -1
        :raise: InvalidSketchError if check_call was True and failed, IncompletePathError if path is not complete
        """

        if idx >= len(path):
            raise IncompletePathError
        while path[idx][0] != 'STOP':
            node, edge = path[idx]
            if check_call:
                if node in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']:
                    raise InvalidSketchError
                idx += 1
                if idx >= len(path):
                    raise IncompletePathError
                continue
            if edge == SIBLING_EDGE:
                idx += 1
                if idx >= len(path):
                    raise IncompletePathError
                continue
            if node == 'DBranch':
                self.consume_DBranch(path, idx)
                return -1
            elif node == 'DExcept':
                self.consume_DExcept(path, idx)
                return -1
            elif node == 'DLoop':
                self.consume_DLoop(path, idx)
                return -1
            else:
                raise ValueError('Invalid node/edge: ' + str((node, edge)))
        return idx

    def consume_DBranch(self, path, idx):
        """
        Consumes a branch node at the given index in the path

        :param path: the path to consume
        :param idx: index of the branch node
        :raise: InvalidSketchError if there is a parse error in the path, IncompletePathError if path is not complete
        """
        idx = self.consume_until_STOP(path, idx+1, check_call=True)
        if idx > 0:
            idx = self.consume_until_STOP(path, idx+1)
            if idx > 0:
                self.consume_until_STOP(path, idx+1)

    def consume_DExcept(self, path, idx):
        """
        Consumes an except node at the given index in the path

        :param path: the path to consume
        :param idx: index of the except node
        :raise: InvalidSketchError if there is a parse error in the path, IncompletePathError if path is not complete
        """
        idx = self.consume_until_STOP(path, idx+1)
        if idx > 0:
            self.consume_until_STOP(path, idx+1)

    def consume_DLoop(self, path, idx):
        """
        Consumes a loop node at the given index in the path

        :param path: the path to consume
        :param idx: index of the loop node
        :raise: InvalidSketchError if there is a parse error in the path, IncompletePathError if path is not complete
        """
        idx = self.consume_until_STOP(path, idx+1, check_call=True)
        if idx > 0:
            self.consume_until_STOP(path, idx+1)

    def paths_to_ast(self, paths):
        """
        Converts a given set of paths into an AST

        :param paths: the set of paths
        :return: the AST
        """
        nodes = []
        ast = {'node': 'DSubTree', '_nodes': nodes}
        for path in paths:
            self.update_until_STOP(nodes, path, 1)
        return ast

    def update_until_STOP(self, nodes, path, pathidx):
        """
        Updates the given list of AST nodes with those along the path starting from pathidx until STOP is reached.
        If a DBranch, DExcept or DLoop is seen midway when going through the path, recursively updates the respective
        node type.

        :param nodes: the list of AST nodes to update
        :param path: the path
        :param pathidx: index of path at which update should start
        :return: the index at which STOP was encountered if there were no recursive updates, otherwise -1
        """
        nodeidx = 0

        while path[pathidx][0] != 'STOP':
            node, edge = path[pathidx]

            if nodeidx >= len(nodes):
                astnode = {}
                if node == 'DBranch':
                    astnode['node'] = node
                    astnode['_cond'] = []
                    astnode['_then'] = []
                    astnode['_else'] = []
                    nodes.append(astnode)
                elif node == 'DExcept':
                    astnode['node'] = node
                    astnode['_try'] = []
                    astnode['_catch'] = []
                    nodes.append(astnode)
                elif node == 'DLoop':
                    astnode['node'] = node
                    astnode['_cond'] = []
                    astnode['_body'] = []
                    nodes.append(astnode)
                else:
                    nodes.append({'node': 'DAPICall', '_call': node})
                    nodeidx += 1
                    pathidx += 1
                    continue
            else:
                astnode = nodes[nodeidx]

            if edge == SIBLING_EDGE:
                nodeidx += 1
                pathidx += 1
                continue

            if node == 'DBranch':
                self.update_DBranch(astnode, path, pathidx)
                return -1
            elif node == 'DExcept':
                self.update_DExcept(astnode, path, pathidx)
                return -1
            elif node == 'DLoop':
                self.update_DLoop(astnode, path, pathidx)
                return -1
            else:
                raise ValueError('Invalid node/edge: ' + str((node, edge)))

        return pathidx

    def update_DBranch(self, astnode, path, pathidx):
        """
        Updates a DBranch AST node with nodes from the path starting at pathidx

        :param astnode: the AST node to update
        :param path: the path
        :param pathidx: index of path at which update should start
        """
        pathidx = self.update_until_STOP(astnode['_cond'], path, pathidx+1)
        if pathidx > 0:
            self.update_until_STOP(astnode['_then'], path, pathidx+1)
            if pathidx > 0:
                self.update_until_STOP(astnode['_else'], path, pathidx+1)

    def update_DExcept(self, astnode, path, pathidx):
        """
        Updates a DExcept AST node with nodes from the path starting at pathidx

        :param astnode: the AST node to update
        :param path: the path
        :param pathidx: index of path at which update should start
        """
        pathidx = self.update_until_STOP(astnode['_try'], path, pathidx+1)
        if pathidx > 0:
            self.update_until_STOP(astnode['_catch'], path, pathidx+1)

    def update_DLoop(self, astnode, path, pathidx):
        """
        Updates a DLoop AST node with nodes from the path starting at pathidx

        :param astnode: the AST node to update
        :param path: the path
        :param pathidx: index of path at which update should start
        """
        pathidx = self.update_until_STOP(astnode['_cond'], path, pathidx+1)
        if pathidx > 0:
            self.update_until_STOP(astnode['_body'], path, pathidx+1)

