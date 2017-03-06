import tensorflow as tf
import numpy as np

from encdec import Encoder, Decoder
from data_reader import CHILD_EDGE, SIBLING_EDGE

class Model():
    def __init__(self, args, infer=False):
        if not infer:
            print('compiling the model')
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
        self.probs_b = [tf.nn.softmax(self.decoder.types_output[i]) for i in
                            range(args.num_predicates)]

        # placeholders for target outputs
        self.targets = tf.placeholder(tf.int32, [args.batch_size, args.max_ast_depth])
        self.targets_p = [tf.placeholder(tf.int32, [args.batch_size, args.max_arity],
                                name='p{0}'.format(i)) for i in range(args.num_predicates)]

        # define losses
        def bool_loss(outputs, targets, mask):
            return tf.reduce_mean(tf.nn.sparse_softmax_cross_entropy_with_logits(outputs, targets))

        self.type_loss = tf.reduce_sum(tf.pack([bool_loss(self.decoder.types_output[i],
                            self.targets_p[i]) for i in range(args.num_predicates)]))
        self.sequence_loss = tf.nn.seq2seq.sequence_loss([logits],
                                    [tf.reshape(self.targets, [-1])],
                                    [tf.ones([args.batch_size * args.max_ast_depth])])

        # define the optimizer (give more weight to sequence_loss)
        self.cost = args.num_predicates * self.sequence_loss + self.type_loss
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
            [probs, state, probs_b] = sess.run([self.probs, self.decoder.state, self.probs_b], feed)

        dist = probs[0]
        dist_preds = probs_b
        return dist, dist_preds
