import tensorflow as tf
from tensorflow.contrib import legacy_seq2seq
import numpy as np

from variational.architecture import VariationalEncoder, VariationalDecoder
from variational.data_reader import CHILD_EDGE, SIBLING_EDGE

class Model():
    def __init__(self, config, infer=False):
        self.config = config
        if infer:
            config.batch_size = 1
            config.decoder.max_ast_depth = 1

        if config.cell == 'lstm':
            for ev in config.evidence:
                ev.rnn_units = int(ev.rnn_units / 2)
            config.decoder.rnn_units = int(config.decoder.rnn_units / 2)

        # setup the encoder
        self.encoder = VariationalEncoder(config)

        # sample from Normal(0,1) and reparameterize
        samples = tf.random_normal([config.batch_size, config.latent_size], 0., 1., tf.float32)
        self.psi = self.encoder.psi_mean + (self.encoder.psi_stdv * samples)

        # setup the decoder with psi as the initial state
        lift_w = tf.get_variable('lift_w', [config.latent_size, config.decoder.rnn_units
                                                            * (2 if config.cell == 'lstm' else 1)])
        lift_b = tf.get_variable('lift_b', [config.decoder.rnn_units
                                                            * (2 if config.cell == 'lstm' else 1)])
        self.initial_state = tf.nn.xw_plus_b(self.psi, lift_w, lift_b)
        self.decoder = VariationalDecoder(config, initial_state=self.initial_state, infer=infer)

        # get the decoder outputs
        output = tf.reshape(tf.concat(self.decoder.outputs, 1), [-1, self.decoder.cell.output_size])
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

    def infer_psi(self, sess, seqs, kws, input_vocab_seqs, input_vocab_kws):
        # apply the dict on inputs (batch_size is 1 during inference)
        x = np.zeros((1, self.config.max_seqs, self.config.max_seq_length, 1), dtype=np.int32)
        k = np.zeros((1, self.config.max_keywords, 1, 1), dtype=np.int32)
        for i, seq in enumerate(seqs):
            x[0, i, :len(seq), 0] = list(map(input_vocab_seqs.get, seq))
        k[0, :len(kws), 0, 0] = list(map(input_vocab_kws.get, kws))

        # reshape into list of tensors
        x = [x[:, i, :, :] for i in range(self.config.max_seqs)]
        k = [k[:, i, :, :] for i in range(self.config.max_keywords)]

        # setup initial states and feed
        feed = {
                self.encoder.seqs_cell_mean_init: self.encoder.seqs_cell_mean_init.eval(session=sess),
                self.encoder.kw_cell_mean_init: self.encoder.kw_cell_mean_init.eval(session=sess),
                self.encoder.seqs_cell_stdv_init: self.encoder.seqs_cell_stdv_init.eval(session=sess),
                self.encoder.kw_cell_stdv_init: self.encoder.kw_cell_stdv_init.eval(session=sess)
               }
        for i in range(self.config.max_seqs):
            feed[self.encoder.seqs[i].name] = x[i]
        for i in range(self.config.max_keywords):
            feed[self.encoder.keywords[i].name] = k[i]
        psi = sess.run(self.psi, feed)
        return psi

    def infer_ast(self, sess, psi, nodes, edges, target_vocab):
        # run the encoder (or use the given psi) and get decoder's start state
        state = sess.run(self.initial_state, { self.psi: psi })

        # run the decoder for every time step
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
