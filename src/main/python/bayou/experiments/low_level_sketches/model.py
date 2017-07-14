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

from bayou.experiments.low_level_sketches.architecture import BayesianEncoder, BayesianDecoder


class Model():
    def __init__(self, config, infer=False):
        self.config = config
        if infer:
            config.batch_size = 1
            config.decoder.max_tokens = 1

        # setup the encoder
        self.encoder = BayesianEncoder(config)
        samples = tf.random_normal([config.batch_size, config.latent_size],
                                   mean=0., stddev=1., dtype=tf.float32)
        self.psi = self.encoder.psi_mean + tf.sqrt(self.encoder.psi_covariance) * samples

        # setup the decoder with psi as the initial state
        lift_w = tf.get_variable('lift_w', [config.latent_size, config.decoder.units])
        lift_b = tf.get_variable('lift_b', [config.decoder.units])
        self.initial_state = tf.nn.xw_plus_b(self.psi, lift_w, lift_b)
        self.decoder = BayesianDecoder(config, initial_state=self.initial_state, infer=infer)

        # get the decoder outputs
        output = tf.reshape(tf.concat(self.decoder.outputs, 1),
                            [-1, self.decoder.cell.output_size])
        logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
        self.probs = tf.nn.softmax(logits)

        # 1. generation loss: log P(X | \Psi)
        self.targets = tf.placeholder(tf.int32, [config.batch_size, config.decoder.max_tokens])
        self.gen_loss = seq2seq.sequence_loss([logits], [tf.reshape(self.targets, [-1])],
                                              [tf.ones([config.batch_size * config.decoder.max_tokens])])

        # 2. latent loss: KL-divergence between P(\Psi | f(\Theta)) and P(\Psi)
        latent_loss = 0.5 * tf.reduce_sum(- tf.log(self.encoder.psi_covariance)
                                          - 1 + self.encoder.psi_covariance
                                          + tf.square(self.encoder.psi_mean), axis=1)
        self.latent_loss = config.alpha * latent_loss

        # 3. evidence loss: log P(f(\theta) | \Psi; \sigma)
        evidence_loss = [ev.evidence_loss(self.psi, encoding, config) for ev, encoding
                         in zip(config.evidence, self.encoder.encodings)]
        evidence_loss = [tf.reduce_sum(loss, axis=1) for loss in evidence_loss]
        self.evidence_loss = config.beta * tf.reduce_sum(tf.stack(evidence_loss), axis=0)

        # The optimizer
        self.loss = self.gen_loss + self.latent_loss + self.evidence_loss
        self.train_op = tf.train.AdamOptimizer(config.learning_rate).minimize(self.loss)

        var_params = [np.prod([dim.value for dim in var.get_shape()])
                      for var in tf.trainable_variables()]
        if not infer:
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

    def infer_ast(self, sess, psi, tokens):
        # use the given psi and get decoder's start state
        state = sess.run(self.initial_state, {self.psi: psi})

        # run the decoder for every time step
        for token in tokens:
            t = np.array([self.config.decoder.vocab[token]], dtype=np.int32)

            feed = {self.decoder.initial_state: state,
                    self.decoder.tokens[0].name: t}
            [probs, state] = sess.run([self.probs, self.decoder.state], feed)

        dist = probs[0]
        return dist
