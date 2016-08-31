import tensorflow as tf
from tensorflow.python.ops import rnn_cell
from tensorflow.python.ops import seq2seq

import decoder
import numpy as np

class Model():
    def __init__(self, args, infer=False):
        self.args = args
        if infer:
            args.batch_size = 1
            args.seq_length = 1

        self.icell = rnn_cell.BasicLSTMCell(args.rnn_size)
        self.icell = rnn_cell.MultiRNNCell([self.icell] * args.num_layers)
        self.ccell = rnn_cell.BasicLSTMCell(args.rnn_size)
        self.ccell = rnn_cell.MultiRNNCell([self.ccell] * args.num_layers)

        self.input_data = [tf.placeholder(tf.int32, [args.batch_size], name='input{0}'.format(i))
                for i in range(args.seq_length)]
        self.caps_data = [tf.placeholder(tf.bool, [args.batch_size], name='caps{0}'.format(i))
                for i in range(args.seq_length)]
        self.targets = tf.placeholder(tf.int32, [args.batch_size, args.seq_length])
        self.initial_state = self.icell.zero_state(args.batch_size, tf.float32)

        softmax_w = tf.get_variable("softmax_w", [args.rnn_size, args.vocab_size])
        softmax_b = tf.get_variable("softmax_b", [args.vocab_size])

        outputs, last_state = decoder.embedding_rnn_decoder(self.input_data, self.caps_data, self.initial_state, self.icell, self.ccell, args.vocab_size, args.rnn_size, (softmax_w,softmax_b), feed_previous=infer)
        output = tf.reshape(tf.concat(1, outputs), [-1, args.rnn_size])
        self.logits = tf.matmul(output, softmax_w) + softmax_b
        self.probs = tf.nn.softmax(self.logits)
        self.cost = seq2seq.sequence_loss([self.logits],
                [tf.reshape(self.targets, [-1])],
                [tf.ones([args.batch_size * args.seq_length])])
        self.final_state = last_state
        self.train_op = tf.train.AdamOptimizer(args.learning_rate).minimize(self.cost)

        var_params = [np.prod([dim.value for dim in var.get_shape()]) for var in tf.trainable_variables()]
        print('Model parameters: {}'.format(np.sum(var_params)))

    def sample(self, sess, chars, vocab, num=200, prime='The '):
        state = self.icell.zero_state(1, tf.float32).eval()
        for char in prime[:-1]:
            x = np.zeros((1, 1))
            x[0, 0] = vocab[char]
            feed = {self.initial_state:state}
            for i in range(self.args.seq_length):
                feed[self.input_data[i]] = x[i]
            [state] = sess.run([self.final_state], feed)

        def weighted_pick(weights):
            t = np.cumsum(weights)
            s = np.sum(weights)
            return(int(np.searchsorted(t, np.random.rand(1)*s)))

        ret = prime
        char = prime[-1]
        for n in range(num):
            x = np.zeros((1, 1))
            x[0, 0] = vocab[char]
            feed = {self.initial_state:state}
            for i in range(self.args.seq_length):
                feed[self.input_data[i].name] = x[i]
            [probs, state] = sess.run([self.probs, self.final_state], feed)
            p = probs[0]

            sample = weighted_pick(p)
            pred = chars[sample]
            ret += pred
            char = pred
        return ret


