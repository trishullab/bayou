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
            # resulting tensor =

            # 4. compute the mean of non-zero encodings
            self.psi_mean = tf.reduce_sum(encodings, axis=0) / denom

        # Compute the covariance of Psi
        with tf.variable_scope('covariance'):
            I = tf.ones([config.batch_size, config.latent_size], dtype=tf.float32)
            self.psi_covariance = I / denom


class BayesianDecoder(object):
    def __init__(self, config, initial_state, psi, infer=False):

        cells1, cells2 = [], []
        for _ in range(config.decoder.num_layers):
            cells1.append(tf.nn.rnn_cell.GRUCell(config.decoder.units))
            cells2.append(tf.nn.rnn_cell.GRUCell(config.decoder.units))
        self.cell1 = tf.nn.rnn_cell.MultiRNNCell(cells1)
        self.cell2 = tf.nn.rnn_cell.MultiRNNCell(cells2)

        # # psi.shape=(batch_size, latent_size) ==> (batch_size, latent_size, 1)
        # attention_memory = tf.reshape(psi, [config.batch_size, config.latent_size, 1])
        # attention_mechanism = tf.contrib.seq2seq.LuongAttention(1, attention_memory)
        # self.cell1 = tf.contrib.seq2seq.AttentionWrapper(self.cell1, attention_mechanism, 1)
        # self.cell2 = tf.contrib.seq2seq.AttentionWrapper(self.cell2, attention_mechanism, 1)
        #
        # # attention stuff above

        # luong attention, location-based alignment function
        # psi.shape = (batch_size, latent_size)
        align_w = tf.get_variable('align_w', shape=(config.decoder.units, config.latent_size))
        # TODO: to minimize the current modification, use cell size as attention output vector size
        att_out_w = tf.get_variable(
            'att_out_w', shape=(config.latent_size + self.cell1.output_size, self.cell1.output_size))

        # placeholders
        self.initial_state = [initial_state] * config.decoder.num_layers
        self.nodes = [tf.placeholder(tf.int32, [config.batch_size], name='node{0}'.format(i))
                      for i in range(config.decoder.max_ast_depth)]
        self.edges = [tf.placeholder(tf.bool, [config.batch_size], name='edge{0}'.format(i))
                      for i in range(config.decoder.max_ast_depth)]

        # projection matrices for output
        self.projection_w = tf.get_variable('projection_w', [self.cell1.output_size,
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
            emb_inp = (tf.nn.embedding_lookup(emb, i) for i in self.nodes)

            # the decoder (modified from tensorflow's seq2seq library to fit tree RNNs)
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
                    with tf.variable_scope('cell1'):  # handles CHILD_EDGE
                        output1, state1 = self.cell1(inp, self.state)
                    with tf.variable_scope('cell2'):  # handles SIBLING_EDGE
                        output2, state2 = self.cell2(inp, self.state)
                    output = tf.where(self.edges[i], output1, output2)
                    self.state = [tf.where(self.edges[i], state1[j], state2[j])
                                  for j in range(config.decoder.num_layers)]

                    # combine attention and output to produce new_output
                    # output.shape = [batch_size, self.output_size]
                    align = tf.matmul(output, align_w)
                    context = align * psi
                    concat = tf.concat([context, output], axis=1)
                    output = tf.tanh(tf.matmul(concat, att_out_w))

                    self.outputs.append(output)
                    if loop_function is not None:
                        prev = output
