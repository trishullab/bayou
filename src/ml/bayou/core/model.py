import tensorflow as tf
from tensorflow.contrib import legacy_seq2seq
import numpy as np

from bayou.core.architecture import BayesianEncoder, BayesianDecoder
from bayou.core.data_reader import CHILD_EDGE, SIBLING_EDGE


class Model():
    def __init__(self, config, infer=False):
        self.config = config
        if infer:
            config.batch_size = 1
            config.decoder.max_ast_depth = 1

        # setup the encoder
        self.encoder = BayesianEncoder(config)
        samples = tf.random_normal([config.batch_size, config.latent_size],
                                   mean=config.latent_mean,
                                   stddev=config.latent_stdv,
                                   dtype=tf.float32)
        self.psi = self.encoder.psi_mean + self.encoder.psi_stdv * samples

        # setup the decoder with psi as the initial state
        lift_w = tf.get_variable('lift_w', [config.latent_size, config.decoder.units])
        lift_b = tf.get_variable('lift_b', [config.decoder.units])
        self.initial_state = tf.nn.xw_plus_b(self.psi, lift_w, lift_b)
        self.decoder = BayesianDecoder(config, initial_state=self.initial_state, infer=infer)

        # get the decoder outputs
        output = tf.reshape(tf.concat(self.decoder.outputs, 1),
                            [-1, self.decoder.cell1.output_size])
        logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
        self.probs = tf.nn.softmax(logits)

        # define losses
        self.targets = tf.placeholder(tf.int32, [config.batch_size, config.decoder.max_ast_depth])

        """ KL-divergence between two Normal distributions: N(M, S) and N(m, s):
        1/2 * ( log(s^2 / S^2) - 1 + (S^2 + (M-m)^2)/s^2 )
        """
        self.latent_loss = 0.5 * tf.reduce_sum(
                                     tf.log(tf.square(config.latent_stdv))
                                     - tf.log(tf.square(self.encoder.psi_stdv)) - 1
                                     + ((tf.square(self.encoder.psi_stdv)
                                        + tf.square(self.encoder.psi_mean - config.latent_mean))
                                        / tf.square(config.latent_stdv)), 1)

        self.gen_loss = legacy_seq2seq.sequence_loss([logits], [tf.reshape(self.targets, [-1])],
                                                     [tf.ones([config.batch_size *
                                                               config.decoder.max_ast_depth])])
        self.loss = tf.reduce_mean(self.gen_loss + self.latent_loss / config.weight_loss)
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

    def infer_ast(self, sess, psi, nodes, edges):
        # use the given psi and get decoder's start state
        state = sess.run(self.initial_state, {self.psi: psi})

        # run the decoder for every time step
        for node, edge in zip(nodes, edges):
            assert edge == CHILD_EDGE or edge == SIBLING_EDGE, 'invalid edge: {}'.format(edge)
            n = np.array([self.config.decoder.vocab[node]], dtype=np.int32)
            e = np.array([edge == CHILD_EDGE], dtype=np.bool)

            feed = {self.decoder.initial_state: state,
                    self.decoder.nodes[0].name: n,
                    self.decoder.edges[0].name: e}
            [probs, state] = sess.run([self.probs, self.decoder.state], feed)

        dist = probs[0]
        return dist
