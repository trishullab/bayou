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
        self.psi = self.encoder.psi_mean + self.encoder.psi_stdv # sampling done in encoder

        # setup the decoder with psi as the initial state
        lift_w = tf.get_variable('lift_w', [config.latent_size, config.decoder.rnn_units
                                                            * (2 if config.cell == 'lstm' else 1)])
        lift_b = tf.get_variable('lift_b', [config.decoder.rnn_units
                                                            * (2 if config.cell == 'lstm' else 1)])
        self.initial_state = tf.nn.xw_plus_b(self.psi, lift_w, lift_b)
        self.decoder = BayesianDecoder(config, initial_state=self.initial_state, infer=infer)

        # get the decoder outputs
        output = tf.reshape(tf.concat(self.decoder.outputs, 1), [-1,self.decoder.cell1.output_size])
        logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
        self.probs = tf.nn.softmax(logits)

        # define losses
        self.targets = tf.placeholder(tf.int32, [config.batch_size, config.decoder.max_ast_depth])
        self.latent_loss = 0.5 * tf.reduce_sum(tf.square(self.encoder.psi_mean)
                                    + tf.square(self.encoder.psi_stdv)
                                    - tf.log(tf.square(self.encoder.psi_stdv)) - 1, 1)
        self.generation_loss = legacy_seq2seq.sequence_loss([logits],
                                    [tf.reshape(self.targets, [-1])],
                                    [tf.ones([config.batch_size * config.decoder.max_ast_depth])])
        self.cost = tf.reduce_mean(self.generation_loss + self.latent_loss/config.weight_loss)
        self.train_op = tf.train.AdamOptimizer(config.learning_rate).minimize(self.cost)

        var_params = [np.prod([dim.value for dim in var.get_shape()])
                            for var in tf.trainable_variables()]
        if not infer:
            print('Model parameters: {}'.format(np.sum(var_params)))

    def infer_psi(self, sess, evidences, feed_only=False):
        if feed_only:
            inputs = evidences
        else:
            # read, wrangle (with batch_size 1) and reshape the data
            inputs = [ev.reshape(ev.wrangle([ev.read_data(evidences)])) for ev in
                        self.config.evidence]

        # setup initial states and feed
        feed = {}
        for j, ev in enumerate(self.config.evidence):
            for k in range(ev.max_num):
                feed[self.encoder.inputs[j][k].name] = inputs[j][k]
        for cell_init in self.encoder.init:
            feed[cell_init] = cell_init.eval(session=sess)
        psi = sess.run(self.psi, feed)
        return psi

    def infer_ast(self, sess, psi, nodes, edges):
        # run the encoder (or use the given psi) and get decoder's start state
        state = sess.run(self.initial_state, { self.psi: psi })

        # run the decoder for every time step
        for node, edge in zip(nodes, edges):
            assert edge == CHILD_EDGE or edge == SIBLING_EDGE, 'invalid edge: {}'.format(edge)
            n = np.array([self.config.decoder.vocab[node]], dtype=np.int32)
            e = np.array([edge == CHILD_EDGE], dtype=np.bool)

            feed = { self.decoder.initial_state: state,
                     self.decoder.nodes[0].name: n,
                     self.decoder.edges[0].name: e }
            [probs, state] = sess.run([self.probs, self.decoder.state], feed)

        dist = probs[0]
        return dist
