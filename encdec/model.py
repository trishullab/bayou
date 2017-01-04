import tensorflow as tf
import numpy as np

from encdec import Encoder, Decoder
from data_reader import CHILD_EDGE, SIBLING_EDGE

class Model():
    def __init__(self, args, infer=False):
        self.args = args
        if infer:
            args.batch_size = 1
            args.max_ast_depth = 1

        # setup the encoder-decoder network
        self.encoder = Encoder(args)
        self.decoder = Decoder(args, initial_state=self.encoder.encoding, infer=infer)

        # get the decoder outputs
        output = tf.reshape(tf.concat(1, self.decoder.outputs), [-1, args.rnn_size])
        logits = tf.matmul(output, self.decoder.projection_w) + self.decoder.projection_b
        self.probs = tf.nn.softmax(logits)

        # define loss
        self.targets = tf.placeholder(tf.int32, [args.batch_size, args.max_ast_depth])
        self.cost = tf.nn.seq2seq.sequence_loss([logits],
                                    [tf.reshape(self.targets, [-1])],
                                    [tf.ones([args.batch_size * args.max_ast_depth])])
        self.train_op = tf.train.AdamOptimizer(args.learning_rate).minimize(self.cost)

        var_params = [np.prod([dim.value for dim in var.get_shape()])
                            for var in tf.trainable_variables()]
        if not infer:
            print('Model parameters: {}'.format(np.sum(var_params)))

    def probability(self, sess, seq, nodes, edges, input_vocab, target_vocab):

        # apply the dict on inputs (batch_size is 1 during inference)
        x = np.zeros((1, self.args.max_seq_length, 1), dtype=np.int32)
        x[0, :len(seq), 0] = list(map(input_vocab.get, seq))
        l = np.array([len(seq)], dtype=np.int32)

        # setup initial states and feed
        init_state = self.encoder.cell_init.eval()
        feed = { self.encoder.seq: x,
                 self.encoder.seq_length: l,
                 self.encoder.cell_init: init_state }

        # run the encoder and get the encoding
        [state] = sess.run([self.encoder.encoding], feed)

        # run the decoder for every time step (beginning with psi as the initial state)
        for node, edge in zip(nodes, edges):
            assert edge == CHILD_EDGE or edge == SIBLING_EDGE, 'invalid edge: {}'.format(edge)
            n = np.array([target_vocab[node]], dtype=np.int32)
            e = np.array([edge == CHILD_EDGE], dtype=np.bool)

            feed = { self.decoder.initial_state: state,
                     self.decoder.nodes[0].name: n,
                     self.decoder.edges[0].name: e }
            [probs, state] = sess.run([self.probs, self.decoder.state], feed)

        dist = probs[0]
        return dist
