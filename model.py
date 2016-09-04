import tensorflow as tf
from tensorflow.python.ops import rnn_cell
from tensorflow.python.ops import seq2seq

import decoder
from data_reader import CHILD_EDGE, SIBLING_EDGE
import numpy as np

class Model():
    def __init__(self, args, infer=False):
        self.args = args
        if infer:
            args.batch_size = 1
            args.seq_length = 1

        self.cell1 = rnn_cell.MultiRNNCell([rnn_cell.BasicLSTMCell(args.rnn_size)] * args.num_layers)
        self.cell2 = rnn_cell.MultiRNNCell([rnn_cell.BasicLSTMCell(args.rnn_size)] * args.num_layers)

        self.node_data = [tf.placeholder(tf.int32, [args.batch_size], name='node{0}'.format(i))
                for i in range(args.seq_length)]
        self.edge_data = [tf.placeholder(tf.bool, [args.batch_size], name='edge{0}'.format(i))
                for i in range(args.seq_length)]
        self.targets = tf.placeholder(tf.int32, [args.batch_size, args.seq_length])
        self.initial_state = self.cell1.zero_state(args.batch_size, tf.float32)

        softmax_w = tf.get_variable("softmax_w", [args.rnn_size, args.vocab_size])
        softmax_b = tf.get_variable("softmax_b", [args.vocab_size])

        outputs, last_state = decoder.embedding_rnn_decoder(self.node_data, self.edge_data, self.initial_state, self.cell1, self.cell2, args.vocab_size, args.rnn_size, (softmax_w,softmax_b), feed_previous=infer)
        output = tf.reshape(tf.concat(1, outputs), [-1, args.rnn_size])
        self.logits = tf.matmul(output, softmax_w) + softmax_b
        self.probs = tf.nn.softmax(self.logits)
        self.cost = seq2seq.sequence_loss([self.logits],
                [tf.reshape(self.targets, [-1])],
                [tf.ones([args.batch_size * args.seq_length])])
        self.final_state = last_state
        self.train_op = tf.train.AdamOptimizer(args.learning_rate).minimize(self.cost)

        var_params = [np.prod([dim.value for dim in var.get_shape()]) for var in tf.trainable_variables()]
        if not infer:
            print('Model parameters: {}'.format(np.sum(var_params)))

    def predict(self, sess, prime, chars, vocab):

        def weighted_pick(weights):
            t = np.cumsum(weights)
            s = np.sum(weights)
            return(int(np.searchsorted(t, np.random.rand(1)*s)))

        state = self.cell1.zero_state(1, tf.float32).eval()
        for node, edge in prime:
            assert edge == CHILD_EDGE or edge == SIBLING_EDGE, 'invalid edge: {}'.format(edge)
            node_data, edge_data = np.zeros((1,), dtype=np.int32), np.zeros((1,), dtype=np.int32)
            node_data[0] = vocab[node]
            edge_data[0] = edge == CHILD_EDGE

            feed = {self.initial_state: state,
                    self.node_data[0].name: node_data,
                    self.edge_data[0].name: edge_data}
            [probs, state] = sess.run([self.probs, self.final_state], feed)

        dist = probs[0]
        prediction = chars[weighted_pick(dist)]
        return dist, prediction
