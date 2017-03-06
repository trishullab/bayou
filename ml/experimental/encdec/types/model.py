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
        self.probs_b = tf.nn.softmax(self.decoder.output_b)
        self.probs_s2 = tf.nn.softmax(self.decoder.output_s2)

        # define placeholders for target outputs
        self.targets = tf.placeholder(tf.int32, [args.batch_size, args.max_ast_depth])
        self.targets_exists_b = tf.placeholder(tf.bool, [args.batch_size, args.max_arity])
        self.targets_exists_n = tf.placeholder(tf.bool, [args.batch_size, args.max_arity])
        self.targets_exists_s = tf.placeholder(tf.bool, [args.batch_size, args.max_arity])
                    # targets_b and targets_s2 are actually bool, but tensorflow disallows this.
                    # int32 is fine because numpy implicitly converts bool (F/T) to int (0/1)
        self.targets_b = tf.placeholder(tf.int32, [args.batch_size, args.max_arity])
        self.targets_n = tf.placeholder(tf.float32, [args.batch_size, args.max_arity])
        self.targets_s1 = tf.placeholder(tf.float32, [args.batch_size, args.max_arity])
        self.targets_s2 = tf.placeholder(tf.int32, [args.batch_size, args.max_arity])

        # define losses
        def bool_loss(outputs, targets, mask):
            outputs = tf.boolean_mask(outputs, mask)
            targets = tf.boolean_mask(targets, mask)
            return tf.reduce_mean(tf.nn.sparse_softmax_cross_entropy_with_logits(outputs, targets))

        def num_loss(outputs, targets, mask):
            outputs = tf.boolean_mask(outputs, mask)
            targets = tf.boolean_mask(targets, mask)
            return tf.nn.l2_loss(targets - outputs)

        self.loss_b = bool_loss(self.decoder.output_b, self.targets_b, self.targets_exists_b)
        self.loss_n = num_loss(self.decoder.output_n, self.targets_n, self.targets_exists_n)
        self.loss_s1 = num_loss(self.decoder.output_s1, self.targets_s1, self.targets_exists_s)
        self.loss_s2 = bool_loss(self.decoder.output_s2, self.targets_s2, self.targets_exists_s)

        self.type_loss = self.loss_b + self.loss_n + self.loss_s1 + self.loss_s2
        self.sequence_loss = tf.nn.seq2seq.sequence_loss([logits],
                                    [tf.reshape(self.targets, [-1])],
                                    [tf.ones([args.batch_size * args.max_ast_depth])])

        # define the optimizer
        self.cost = self.sequence_loss + self.type_loss
        self.train_op = tf.train.AdamOptimizer(args.learning_rate).minimize(self.cost)

        var_params = [np.prod([dim.value for dim in var.get_shape()])
                            for var in tf.trainable_variables()]
        if not infer:
            print('Model parameters: {}'.format(np.sum(var_params)))

    def infer(self, sess, seqs, nodes, edges, input_vocab, target_vocab):

        # apply the dict on inputs (batch_size is 1 during inference)
        x = np.zeros((1, self.args.max_seqs, self.args.max_seq_length, 1), dtype=np.int32)
        l = np.zeros((1, self.args.max_seqs), dtype=np.int32)
        for i, seq in enumerate(sorted(seqs)):
            x[0, i, :len(seq), 0] = list(map(input_vocab.get, seq))
            l[0, i] = len(seq)

        # reshape into list of tensors
        x = [x[:, i, :, :] for i in range(self.args.max_seqs)]
        l = [l[:, i] for i in range(self.args.max_seqs)]

        # setup initial states and feed
        init_state = self.encoder.cell_init.eval()
        feed = { self.encoder.cell_init: init_state }
        for i in range(self.args.max_seqs):
            feed[self.encoder.seq[i].name] = x[i]
            feed[self.encoder.seq_length[i].name] = l[i]

        # run the encoder and get the encoding
        [state] = sess.run([self.encoder.encoding], feed)

        # run the decoder for every time step (beginning with the initial state obtained above)
        for node, edge in zip(nodes, edges):
            n = np.array([target_vocab[node]], dtype=np.int32)
            e = np.array([edge == CHILD_EDGE], dtype=np.bool)

            feed = { self.decoder.initial_state: state,
                     self.decoder.nodes[0].name: n,
                     self.decoder.edges[0].name: e }
            [probs, state, ob, on, os1, os2] = sess.run([self.probs, self.decoder.state,
                                                 self.probs_b, self.decoder.output_n,
                                                 self.decoder.output_s1, self.probs_s2],
                                                 feed)

        return probs[0], ob[0], on[0], os1[0], os2[0]
