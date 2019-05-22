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

import tensorflow as tf
from tensorflow.contrib import legacy_seq2seq as seq2seq
import numpy as np

from bayou.models.low_level_evidences.architecture import BayesianEncoder, BayesianDecoder
from bayou.models.low_level_evidences.utils import get_var_list


class Model():
    def __init__(self, config, iterator, infer=False, bayou_mode=True):
        assert config.model == 'lle', 'Trying to load different model implementation: ' + config.model
        self.config = config


        newBatch = iterator.get_next()
        nodes, edges, targets = newBatch[:3]
        ev_data = newBatch[3:]

        nodes = tf.transpose(nodes)
        edges = tf.transpose(edges)

        with tf.variable_scope('Embedding'):
            emb = tf.get_variable('emb', [config.decoder.vocab_size, config.decoder.units])


        with tf.variable_scope("Encoder"):

            self.encoder = BayesianEncoder(config, ev_data, infer)
            samples_1 = tf.random_normal([config.batch_size, config.latent_size], mean=0., stddev=1., dtype=tf.float32)

            self.psi_encoder = self.encoder.psi_mean + tf.sqrt(self.encoder.psi_covariance) * samples_1

        # setup the decoder with psi as the initial state
        with tf.variable_scope("Decoder"):

            # emb = tf.get_variable('emb', [config.decoder.vocab_size, config.decoder.units])
            lift_w = tf.get_variable('lift_w', [config.latent_size, config.decoder.units])
            lift_b = tf.get_variable('lift_b', [config.decoder.units])


            initial_state = tf.nn.xw_plus_b(self.psi_encoder, lift_w, lift_b, name="Initial_State")
            self.decoder = BayesianDecoder(config, emb, initial_state, nodes, edges)


        # get the decoder outputs
        with tf.name_scope("Loss"):
            output = tf.reshape(tf.concat(self.decoder.outputs, 1),
                                [-1, self.decoder.cell1.output_size])
            logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
            self.ln_probs = tf.nn.log_softmax(logits)
            self.idx = tf.multinomial(logits, 1)

            # 1. generation loss: log P(Y | Z)
            cond = tf.not_equal(tf.reduce_sum(self.encoder.psi_mean, axis=1), 0)
            cond = tf.reshape( tf.tile(tf.expand_dims(cond, axis=1) , [1,config.decoder.max_ast_depth]) , [-1] )
            cond = tf.where(cond , tf.ones(cond.shape), tf.zeros(cond.shape))


            self.loss = seq2seq.sequence_loss([logits], [tf.reshape(targets, [-1])], [cond])

            self.allEvSigmas = [ ev.sigma for ev in self.config.evidence ]
            #unused if MultiGPU is being used
            with tf.name_scope("train"):
                train_ops = get_var_list()['bayou_vars']

        if not infer:
            opt = tf.train.AdamOptimizer(config.learning_rate)
            self.train_op = opt.minimize(self.loss, var_list=train_ops)

            var_params = [np.prod([dim.value for dim in var.get_shape()])
                          for var in tf.trainable_variables()]
            print('Model parameters: {}'.format(np.sum(var_params)))




    def infer_psi(self, sess, evidences):
        # read and wrangle (with batch_size 1) the data
        inputs = [ev.wrangle([ev.read_data_point(evidences)]) for ev in self.config.evidence]

        # setup initial states and feed
        feed = {}
        for j, ev in enumerate(self.config.evidence):
            feed[self.encoder.inputs[j].name] = inputs[j]
        psi = sess.run(self.psi, feed)
        return psi

    def infer_ast(self, sess, psi, nodes, edges, cache=None):
        # check cache if provided
        # if cache is not None:
        #     serialized = ','.join(['(' + node + ',' + edge + ')' for node, edge in zip(nodes, edges)])
        #     if serialized in cache:
        #         return cache[serialized]

        # use the given psi and get decoder's start state
        state = sess.run(self.initial_state, {self.psi: psi})
        state = [state] * self.config.decoder.num_layers

        # run the decoder for every time step
        for node, edge in zip(nodes, edges):
            assert edge == CHILD_EDGE or edge == SIBLING_EDGE, 'invalid edge: {}'.format(edge)
            n = np.array([self.config.decoder.vocab[node]], dtype=np.int32)
            e = np.array([edge == CHILD_EDGE], dtype=np.bool)

            feed = {self.decoder.nodes[0].name: n,
                    self.decoder.edges[0].name: e}
            for i in range(self.config.decoder.num_layers):
                feed[self.decoder.initial_state[i].name] = state[i]
            [probs, state] = sess.run([self.ln_probs, self.decoder.state], feed)

        dist = probs[0]

        # # save in cache if provided
        # if cache is not None:
        #     cache[serialized] = dist

        return dist
