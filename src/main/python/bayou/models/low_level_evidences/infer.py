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
from copy import deepcopy, copy

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


class Candidate():
    def __init__(self,initial_state):
        self.tree_currNode = Node("DSubTree")
        self.head = self.tree_currNode

        self.last_item = self.tree_currNode.val
        self.last_edge = SIBLING_EDGE
        self.branch_stack = []

        self.length = 1
        self.log_probabilty = -np.inf
        self.state = initial_state

        self.rolling = True



class BayesianPredictor(object):

    def __init__(self, save, sess, config, iterator):
        self.sess = sess

        self.config = config
        # load the saved config
        self.inputs = [ev.placeholder(config) for ev in config.evidence]
        ev_data = self.inputs

        self.nodes = tf.placeholder(tf.int32, shape=(config.batch_size,config.decoder.max_ast_depth))
        self.edges = tf.placeholder(tf.bool, shape=(config.batch_size, config.decoder.max_ast_depth))
        self.targets = tf.placeholder(tf.int32, shape=(config.batch_size, config.decoder.max_ast_depth))

        nodes = tf.transpose(self.nodes)
        edges = tf.transpose(self.edges)

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
            self.decoder = BayesianDecoder(config, emb, self.initial_state, nodes, edges)

        with tf.name_scope("Loss"):
            output = tf.reshape(tf.concat(self.decoder.outputs, 1),
                                [-1, self.decoder.cell1.output_size])
            logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
            self.ln_probs = tf.nn.log_softmax(logits)
            self.idx = tf.multinomial(logits, 1)

            self.top_k_values, self.top_k_indices = tf.nn.top_k(self.ln_probs, k=config.batch_size)


        # restore the saved model
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(save)
        saver.restore(self.sess, ckpt.model_checkpoint_path)





    def get_state(self, evidences, num_psi_samples=100):
        # get the contrib from evidence to the initial state
        rdp = [ev.read_data_point(evidences, infer=True) for ev in self.config.evidence]
        inputs = [ev.wrangle([ev_rdp for k in range(self.config.batch_size)]) for ev, ev_rdp in zip(self.config.evidence, rdp)]

        feed = {}
        for j, ev in enumerate(self.config.evidence):
            feed[self.inputs[j].name] = inputs[j]

        psis = []
        for i in range(num_psi_samples):
            psi = self.sess.run(self.psi_encoder, feed)
            psis.append(psi)
        psi = np.mean(psis, axis=0)

        feed = {self.psi_encoder:psi}
        state = self.sess.run(self.initial_state, feed)

        return state



    def beam_search(self, evidences, topK=10):

        self.config.batch_size = topK

        init_state = self.get_state(evidences)

        #BUG :: one dummy step
        feed = {}
        feed[self.nodes.name] = np.array([[self.config.decoder.vocab['DSubTree']] for k in range(topK) ], dtype=np.int32)
        feed[self.edges.name] = np.array([[SIBLING_EDGE] for k in range(topK)], dtype=np.bool)
        feed[self.initial_state.name] = init_state

        init_state = self.sess.run(self.decoder.state , feed)[0][0]



        candies = [Candidate(init_state) for k in range(topK)]
        candies[0].log_probabilty = -0.0

        i = 0
        while(True):
            # states was batch_size * LSTM_Decoder_state_size
            candies = self.get_next_output_with_fan_out(candies)
            #print([candy.head.dfs() for candy in candies])
            #print([candy.rolling for candy in candies])

            if self.check_for_all_STOP(candies): # branch_stack and last_item
                break

            i+=1

            if i == MAX_GEN_UNTIL_STOP:
                break


        candies.sort(key=lambda x: x.log_probabilty, reverse=True)

        return candies



    def check_for_all_STOP(self, candies):
        for candy in candies:
            if candy.rolling == True:
                return False

        return True



    def get_next_output_with_fan_out(self, candies):

        topK = len(candies)

        last_item = [[self.config.decoder.vocab[candy.last_item]] for candy in candies]
        last_edge = [[candy.last_edge] for candy in candies]
        states = [candy.state for candy in candies]

        feed = {}
        feed[self.nodes.name] = np.array(last_item, dtype=np.int32)
        feed[self.edges.name] = np.array(last_edge, dtype=np.bool)
        feed[self.initial_state.name] = np.array(states)

        [states, beam_ids, beam_ln_probs, top_idx] = self.sess.run([self.decoder.state, self.top_k_indices, self.top_k_values, self.idx] , feed)

        states = states[0]
        next_nodes = [[self.config.decoder.chars[idx] for idx in beam] for beam in beam_ids]


        # states is still topK * LSTM_Decoder_state_size
        # next_node is topK * topK
        # node_probs in  topK * topK
        # log_probabilty is topK

        log_probabilty = np.array([candy.log_probabilty for candy in candies])
        length = np.array([candy.length for candy in candies])

        for i in range(topK):
            if candies[i].rolling == False:
                length[i] = candies[i].length + 1
            else:
               length[i] = candies[i].length

        for i in range(topK): # denotes the candidate
            for j in range(topK): # denotes the items
                if candies[i].rolling == False and j > 0:
                   beam_ln_probs[i][j] = -np.inf
                elif candies[i].rolling == False and j == 0:
                   beam_ln_probs[i][j] = 0.0

        new_probs = log_probabilty[:,None]  + beam_ln_probs

        len_norm_probs = new_probs / np.power(length[:,None], 0.0)

        rows, cols = np.unravel_index(np.argsort(len_norm_probs, axis=None)[::-1], new_probs.shape)
        rows, cols = rows[:topK], cols[:topK]

        # rows mean which of the original candidate was finally selected
        new_candies = []
        for row, col in zip(rows, cols):
            new_candy = deepcopy(candies[row]) #candies[row].copy()
            if new_candy.rolling:
                new_candy.state = states[row]
                new_candy.log_probabilty = new_probs[row][col]
                new_candy.length += 1

                value2add = next_nodes[row][col]
                # print(value2add)


                if new_candy.last_edge == SIBLING_EDGE:
                    new_candy.tree_currNode = new_candy.tree_currNode.addAndProgressSiblingNode(Node(value2add))
                else:
                    new_candy.tree_currNode = new_candy.tree_currNode.addAndProgressChildNode(Node(value2add))


                # before updating the last item lets check for penultimate value
                if new_candy.last_edge == CHILD_EDGE and new_candy.last_item in ['DBranch', 'DExcept', 'DLoop']:
                     new_candy.branch_stack.append(new_candy.tree_currNode)
                     new_candy.last_edge = SIBLING_EDGE
                     new_candy.last_item = value2add

                elif value2add in ['DBranch', 'DExcept', 'DLoop']:
                     new_candy.branch_stack.append(new_candy.tree_currNode)
                     new_candy.last_edge = SIBLING_EDGE
                     new_candy.last_item = value2add

                elif value2add == 'STOP':
                     if len(new_candy.branch_stack) == 0:
                          new_candy.rolling = False
                     else:
                          new_candy.tree_currNode = new_candy.branch_stack.pop()
                          new_candy.last_item = new_candy.tree_currNode.val
                          new_candy.last_edge = CHILD_EDGE
                else:
                     new_candy.last_edge = SIBLING_EDGE
                     new_candy.last_item = value2add

            new_candies.append(new_candy)

        return new_candies







    def random_search(self, evidences):

        # got the state, to be used subsequently
        state = self.get_state(evidences)
        start_node = Node("DSubTree")
        head, final_state = self.consume_siblings_until_STOP(state, start_node)

        return head.sibling


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
        if init_node.val == 'STOP':
            return head

        stack_QUEUE = []

        while True:

            predictionNode, state = self.get_prediction(candidate.val, SIBLING_EDGE, state)
            candidate = candidate.addAndProgressSiblingNode(predictionNode)


            prediction = predictionNode.val
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
