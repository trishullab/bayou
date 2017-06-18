import numpy as np
import tensorflow as tf
from tensorflow.contrib import legacy_seq2seq as seq2seq

from bayou.experiments.nonbayesian.utils import CHILD_EDGE, SIBLING_EDGE
from bayou.experiments.nonbayesian.architecture import NonBayesianEncoder, NonBayesianDecoder


class Model():
    def __init__(self, config, infer=False):
        self.config = config
        if infer:
            config.batch_size = 1
            config.decoder.max_ast_depth = 1

        # setup the encoder
        self.encoder = NonBayesianEncoder(config)

        # setup the decoder with the encoding as the initial state
        self.decoder = NonBayesianDecoder(config, initial_state=self.encoder.encoding, infer=infer)

        # get the decoder outputs
        output = tf.reshape(tf.concat(self.decoder.outputs, 1),
                            [-1, self.decoder.cell1.output_size])
        logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
        self.probs = tf.nn.softmax(logits)

        # 1. generation loss: log P(X | \Psi)
        self.targets = tf.placeholder(tf.int32, [config.batch_size, config.decoder.max_ast_depth])
        self.gen_loss = seq2seq.sequence_loss([logits], [tf.reshape(self.targets, [-1])],
                                              [tf.ones([config.batch_size * config.decoder.max_ast_depth])])

        # The optimizer
        self.loss = self.gen_loss
        self.train_op = tf.train.AdamOptimizer(config.learning_rate).minimize(self.loss)

        var_params = [np.prod([dim.value for dim in var.get_shape()])
                      for var in tf.trainable_variables()]
        if not infer:
            print('Model parameters: {}'.format(np.sum(var_params)))

    def infer_ast(self, sess, evidences, nodes, edges):
        # read and wrangle (with batch_size 1) the data
        inputs = [ev.wrangle([ev.read_data_point(evidences)]) for ev in self.config.evidence]

        # setup initial states and feed
        feed = {}
        for j, ev in enumerate(self.config.evidence):
            feed[self.encoder.inputs[j].name] = inputs[j]
        state = sess.run(self.encoder.encoding, feed)

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
