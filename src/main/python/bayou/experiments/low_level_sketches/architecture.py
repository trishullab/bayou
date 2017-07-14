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
from itertools import chain


class BayesianEncoder(object):
    def __init__(self, config):

        self.inputs = [ev.placeholder(config) for ev in config.evidence]
        exists = [ev.exists(i) for ev, i in zip(config.evidence, self.inputs)]
        zeros = tf.zeros([config.batch_size, config.latent_size], dtype=tf.float32)

        # Compute the denominator used for mean and covariance
        for ev in config.evidence:
            ev.init_sigma(config)
        d = [tf.where(exist, tf.tile([1. / tf.square(ev.sigma)], [config.batch_size]),
                      tf.zeros(config.batch_size)) for ev, exist in zip(config.evidence, exists)]
        d = 1. + tf.reduce_sum(tf.stack(d), axis=0)
        denom = tf.tile(tf.reshape(d, [-1, 1]), [1, config.latent_size])

        # Compute the mean of Psi
        with tf.variable_scope('mean'):
            # 1. compute encoding
            self.encodings = [ev.encode(i, config) for ev, i in zip(config.evidence, self.inputs)]
            encodings = [encoding / tf.square(ev.sigma) for ev, encoding in
                         zip(config.evidence, self.encodings)]

            # 2. pick only encodings from valid inputs that exist, otherwise pick zero encoding
            encodings = [tf.where(exist, enc, zeros) for exist, enc in zip(exists, encodings)]

            # 3. tile the encodings according to each evidence type
            encodings = [[enc] * ev.tile for ev, enc in zip(config.evidence, encodings)]
            encodings = tf.stack(list(chain.from_iterable(encodings)))

            # 4. compute the mean of non-zero encodings
            self.psi_mean = tf.reduce_sum(encodings, axis=0) / denom

        # Compute the covariance of Psi
        with tf.variable_scope('covariance'):
            I = tf.ones([config.batch_size, config.latent_size], dtype=tf.float32)
            self.psi_covariance = I / denom


class BayesianDecoder(object):
    def __init__(self, config, initial_state, infer=False):

        self.cell = tf.nn.rnn_cell.GRUCell(config.decoder.units)

        # placeholders
        self.initial_state = initial_state
        self.tokens = [tf.placeholder(tf.int32, [config.batch_size], name='token{0}'.format(i))
                       for i in range(config.decoder.max_tokens)]

        # projection matrices for output
        self.projection_w = tf.get_variable('projection_w', [self.cell.output_size,
                                                             config.decoder.vocab_size])
        self.projection_b = tf.get_variable('projection_b', [config.decoder.vocab_size])

        # setup embedding
        with tf.variable_scope('decoder'):
            emb = tf.get_variable('emb', [config.decoder.vocab_size, config.decoder.units])

            def loop_fn(prev, _):
                prev = tf.nn.xw_plus_b(prev, self.projection_w, self.projection_b)
                prev_symbol = tf.argmax(prev, 1)
                return tf.nn.embedding_lookup(emb, prev_symbol)

            loop_function = loop_fn if infer else None
            emb_inp = (tf.nn.embedding_lookup(emb, i) for i in self.tokens)

            # TODO: update with dynamic decoder (being implemented in tf) once it is released
            with tf.variable_scope('rnn'):
                self.state = self.initial_state
                self.outputs = []
                prev = None
                for i, inp in enumerate(emb_inp):
                    if loop_function is not None and prev is not None:
                        with tf.variable_scope('loop_function', reuse=True):
                            inp = loop_function(prev, i)
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    with tf.variable_scope('cell'):
                        output, self.state = self.cell(inp, self.state)
                    self.outputs.append(output)
                    if loop_function is not None:
                        prev = output
