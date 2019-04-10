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
from bayou.models.low_level_evidences.architecture import BayesianEncoder, BayesianDecoder
from bayou.models.low_level_evidences.node import CHILD_EDGE, SIBLING_EDGE, Node, get_ast
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

    def __init__(self, save, sess, config, iterator):
        self.sess = sess

        config.batch_size = 1
        config.decoder.max_ast_depth = 1
        self.config = config
        # load the saved config
        self.inputs = [ev.placeholder(config) for ev in config.evidence]
        ev_data = self.inputs

        self.nodes = tf.placeholder(tf.int32, shape=(config.batch_size,config.decoder.max_ast_depth))
        self.parents = tf.placeholder(tf.int32, shape=(config.batch_size, config.decoder.max_ast_depth))
        self.edges = tf.placeholder(tf.bool, shape=(config.batch_size, config.decoder.max_ast_depth))
        self.targets = tf.placeholder(tf.int32, shape=(config.batch_size, config.decoder.max_ast_depth))


        with tf.variable_scope("Encoder"):
            self.encoder = BayesianEncoder(config, ev_data, infer=True)
            samples_1 = tf.random_normal([config.batch_size, config.latent_size], mean=0., stddev=1., dtype=tf.float32)
            self.psi_encoder = self.encoder.psi_mean + tf.sqrt(self.encoder.psi_covariance) * samples_1

        # setup the decoder with psi as the initial state
        with tf.variable_scope("Decoder"):

            emb = tf.get_variable('emb', [config.decoder.vocab_size, config.decoder.units])
            lift_w = tf.get_variable('lift_w', [config.latent_size, config.decoder.units])
            lift_b = tf.get_variable('lift_b', [config.decoder.units])


            self.initial_state = tf.nn.xw_plus_b(self.psi_encoder, lift_w, lift_b, name="Initial_State")
            self.decoder = BayesianDecoder(config, emb, self.initial_state, self.nodes, self.parents, self.edges)

        with tf.name_scope("Loss"):
            output = tf.reshape(tf.concat(self.decoder.outputs, 1),
                                [-1, self.decoder.cell1.output_size])
            logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
            self.ln_probs = tf.nn.log_softmax(logits)
            self.idx = tf.multinomial(logits, 1)


        # restore the saved model
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(save)
        saver.restore(self.sess, ckpt.model_checkpoint_path)





    def get_state(self, evidences):
        # get the contrib from evidence to the initial state
        rdp = [ev.read_data_point(evidences, infer=True) for ev in self.config.evidence]
        inputs = [ev.wrangle([ev_rdp]) for ev, ev_rdp in zip(self.config.evidence, rdp)]

        feed = {}
        for j, ev in enumerate(self.config.evidence):
            feed[self.inputs[j].name] = inputs[j]

        state = self.sess.run(self.initial_state, feed)
        return state



    def beam_search(self, evidences, topK=10):


        # got the state, to be used subsequently
        state = self.get_state(evidences)
        start_node = Node("DSubTree")
        head, final_state = self.consume_siblings_until_STOP(state, start_node)

        return head


    def get_prediction(self, node, edge, state):
        feed = {}
        feed[self.nodes.name] = np.array([[self.config.decoder.vocab[node]]], dtype=np.int32)
        feed[self.edges.name] = np.array([[edge]], dtype=np.bool)
        feed[self.initial_state.name] = state

        [state,idx] = self.sess.run([self.decoder.state, self.idx] , feed)
        idx = idx[0][0]
        state = state[0]
        prediction = self.config.decoder.chars[idx]

        return Node(prediction), state



    def consume_siblings_until_STOP(self, state, init_node):
        # all the candidate solutions starting with a DSubTree node
        head = candidate = init_node
        stack_QUEUE = []

        while True:

            predictionNode, state = self.get_prediction(candidate.val, SIBLING_EDGE, state)
            candidate = candidate.addAndProgressSiblingNode(predictionNode)

            if prediction in ['DBranch', 'DExcept', 'DLoop']:
                stack_QUEUE.append((candidate, prediction))

            elif prediction == 'STOP':

                while len(stack_QUEUE) != 0:
                    q_candidate, branching_type = stack_QUEUE.pop()
                    if branching_type == 'DBranch':
                        q_candidate.child, state = self.consume_DBranch(state)
                    elif branching_type == 'DExcept':
                        q_candidate.child, state = self.consume_DExcept(state)
                    elif branching_type == 'DLoop':
                        q_candidate.child, state = self.consume_DLoop(state)
                #end of inner while
                break

        #END OF WHILE
        return head, state


    def consume_DExcept(self, state):
        tryStatementNode, state = self.get_prediction('DExcept', CHILD_EDGE, state)
        tryBranch , state = self.consume_siblings_until_STOP(state, tryStatementNode)

        catchStartNode, state = self.get_prediction(tryStatementNode.val, CHILD_EDGE, state)
        catchBranch, state = self.consume_siblings_until_STOP(state, catchStartNode)

        tryBranch.child = catchStartNode

        return tryBranch, state



    def consume_DLoop(self, state):
        loopConditionNode, state = self.get_prediction('DLoop', CHILD_EDGE, state)
        loopConditionNode.sibling = Node('STOP')

        loopStartNode, state = self.get_prediction(loopConditionNode.val, CHILD_EDGE, state)
        loopBranch, state = self.consume_siblings_until_STOP(state, loopStartNode)

        loopConditionNode.child = loopBranch

        return loopConditionNode, state



    def consume_DBranch(self, state):
        ifStatementNode, state = self.get_prediction('DBranch', CHILD_EDGE, state)
        ifThenBranch , state = self.consume_siblings_until_STOP(state, ifStatementNode)

        #
        elseBranchStartNode, state = self.get_prediction(ifStatementNode.val, CHILD_EDGE, state)
        elseBranch, state = self.consume_siblings_until_STOP(state, elseBranchStartNode)
        #
        ifThenBranch.child = elseBranch

        return ifThenBranch, state


    def get_a1b1(self, evidences):
        # setup initial states and feed

        rdp = [ev.read_data_point(evidences, infer=True) for ev in self.config.evidence]
        inputs = [ev.wrangle([ev_rdp]) for ev, ev_rdp in zip(self.config.evidence, rdp)]

        feed = {}
        for j, ev in enumerate(self.config.evidence):
            feed[self.inputs[j].name] = inputs[j]


        [  encMean, encCovar ] = self.sess.run([ self.encoder.psi_mean , self.encoder.psi_covariance], feed)

        return encMean[0], encCovar[0]





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
