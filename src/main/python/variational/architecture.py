import tensorflow as tf
from tensorflow.contrib import rnn
from tensorflow.contrib import legacy_seq2seq

class VariationalEncoder(object):
    def __init__(self, args):

        if args.cell == 'lstm':
            seqs_cell = rnn.BasicLSTMCell(args.seqs_rnn_units, state_is_tuple=False)
            kw_cell = rnn.BasicLSTMCell(args.kw_ffnn_units, state_is_tuple=False)
        else:
            seqs_cell = rnn.BasicRNNCell(args.seqs_rnn_units)
            kw_cell = rnn.BasicRNNCell(args.kw_ffnn_units)

        def length(seq):
            elems = tf.sign(tf.reduce_max(seq, axis=2))
            return tf.reduce_sum(elems, axis=1)

        self.seqs = [tf.placeholder(tf.int32, [args.batch_size, args.max_seq_length, 1], 
                            name='seqs{0}'.format(i)) for i in range(args.max_seqs)]
        self.keywords = [tf.placeholder(tf.int32, [args.batch_size, 1, 1], # "RNN" with 1 length
                            name='keywords{0}'.format(i)) for i in range(args.max_keywords)]

        seq_length = [length(seq) for seq in self.seqs]
        kws_length = [length(kw) for kw in self.keywords]
        zero = tf.constant(0, dtype=tf.int32)
        exists_seq_kw = [tf.not_equal(seq_len, zero) for seq_len in seq_length] + \
                        [tf.not_equal(kw_len, zero) for kw_len in kws_length]
        exists_seq_kw += exists_seq_kw[-args.max_keywords:] * args.kw_weight
        num_seqs_kws = tf.count_nonzero(tf.stack(exists_seq_kw), axis=0, dtype=tf.float32)
        num_seqs_kws = tf.tile(tf.reshape(num_seqs_kws, [-1, 1]), [1, args.latent_size])
        all_zeros = tf.zeros([args.batch_size, args.latent_size], dtype=tf.float32)

        # mean encoder
        with tf.variable_scope('variational_encoder_mean'):
            latent_encodings = []

            # RNN for sequences
            with tf.variable_scope('seqs_rnn'):
                seqs_cell_mean = rnn.EmbeddingWrapper(seqs_cell,
                                    embedding_classes=args.input_vocab_seqs_size,
                                    embedding_size=args.seqs_rnn_units)
                self.seqs_cell_mean_init = seqs_cell_mean.zero_state(args.batch_size, tf.float32)
                w_mean_seqs = tf.get_variable('w_mean_seqs', [seqs_cell_mean.state_size,
                                                                args.latent_size])
                b_mean_seqs = tf.get_variable('b_mean_seqs', [args.latent_size])
                for i, seq in enumerate(self.seqs):
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    _, encoding = tf.nn.dynamic_rnn(seqs_cell_mean, seq,
                                            sequence_length=seq_length[i],
                                            initial_state=self.seqs_cell_mean_init,
                                            dtype=tf.float32)
                    latent_encoding = tf.nn.xw_plus_b(encoding, w_mean_seqs, b_mean_seqs)
                    latent_encodings.append(latent_encoding)

            # FFNN for keywords
            with tf.variable_scope('kw_ffnn'):
                kw_cell_mean = rnn.EmbeddingWrapper(kw_cell,
                                    embedding_classes=args.input_vocab_kws_size,
                                    embedding_size=args.kw_ffnn_units)
                self.kw_cell_mean_init = kw_cell_mean.zero_state(args.batch_size, tf.float32)
                w_mean_kw = tf.get_variable('w_mean_kw', [kw_cell_mean.state_size,
                                                                args.latent_size])
                b_mean_kw = tf.get_variable('b_mean_kw', [args.latent_size])
                for i, kw in enumerate(self.keywords):
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    _, encoding = tf.nn.dynamic_rnn(kw_cell_mean, kw,
                                            sequence_length=kws_length[i],
                                            initial_state=self.kw_cell_mean_init,
                                            dtype=tf.float32)
                    latent_encoding = tf.nn.xw_plus_b(encoding, w_mean_kw, b_mean_kw)
                    latent_encodings.append(latent_encoding)
                latent_encodings += latent_encodings[-args.max_keywords:] * args.kw_weight

            assert len(latent_encodings) == len(exists_seq_kw)
            latent_encodings = [tf.where(exists, encoding, all_zeros) for exists, encoding in
                                    zip(exists_seq_kw, latent_encodings)]
            sum_latent_encodings = tf.reduce_sum(tf.stack(latent_encodings), axis=0)
            self.psi_mean = tf.divide(sum_latent_encodings, num_seqs_kws)

        # stdv encoder
        with tf.variable_scope('variational_encoder_stdv'):
            latent_encodings = []

            # RNN for sequences
            with tf.variable_scope('seqs_rnn'):
                seqs_cell_stdv = rnn.EmbeddingWrapper(seqs_cell,
                                    embedding_classes=args.input_vocab_seqs_size,
                                    embedding_size=args.seqs_rnn_units)
                self.seqs_cell_stdv_init = seqs_cell_stdv.zero_state(args.batch_size, tf.float32)
                w_stdv_seqs = tf.get_variable('w_stdv_seqs', [seqs_cell_stdv.state_size,
                                                                args.latent_size])
                b_stdv_seqs = tf.get_variable('b_stdv_seqs', [args.latent_size])
                for i, seq in enumerate(self.seqs):
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    _, encoding = tf.nn.dynamic_rnn(seqs_cell_stdv, seq,
                                            sequence_length=seq_length[i],
                                            initial_state=self.seqs_cell_stdv_init,
                                            dtype=tf.float32)
                    latent_encoding = tf.nn.xw_plus_b(encoding, w_stdv_seqs, b_stdv_seqs)
                    latent_encodings.append(latent_encoding)

            # FFNN for keywords
            with tf.variable_scope('kw_ffnn'):
                kw_cell_stdv = rnn.EmbeddingWrapper(kw_cell,
                                    embedding_classes=args.input_vocab_kws_size,
                                    embedding_size=args.kw_ffnn_units)
                self.kw_cell_stdv_init = kw_cell_stdv.zero_state(args.batch_size, tf.float32)
                w_stdv_kw = tf.get_variable('w_stdv_kw', [kw_cell_stdv.state_size,
                                                                args.latent_size])
                b_stdv_kw = tf.get_variable('b_stdv_kw', [args.latent_size])
                for i, kw in enumerate(self.keywords):
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    _, encoding = tf.nn.dynamic_rnn(kw_cell_stdv, kw,
                                            sequence_length=kws_length[i],
                                            initial_state=self.kw_cell_stdv_init,
                                            dtype=tf.float32)
                    latent_encoding = tf.nn.xw_plus_b(encoding, w_stdv_kw, b_stdv_kw)
                    latent_encodings.append(latent_encoding)
                latent_encodings += latent_encodings[-args.max_keywords:] * args.kw_weight

            assert len(latent_encodings) == len(exists_seq_kw)
            latent_encodings = [tf.where(exists, encoding, all_zeros) for exists, encoding in
                                    zip(exists_seq_kw, latent_encodings)]
            sum_latent_encodings = tf.reduce_sum(tf.stack(latent_encodings), axis=0)
            self.psi_stdv = tf.divide(sum_latent_encodings, num_seqs_kws)


class VariationalDecoder(object):
    def __init__(self, args, initial_state, infer=False):

        if args.cell == 'lstm':
            self.cell = rnn.BasicLSTMCell(args.decoder_rnn_units, state_is_tuple=False)
        else:
            self.cell = rnn.BasicRNNCell(args.decoder_rnn_units)

        # placeholders
        self.initial_state = initial_state
        self.nodes = [tf.placeholder(tf.int32, [args.batch_size], name='node{0}'.format(i))
                            for i in range(args.max_ast_depth)]
        self.edges = [tf.placeholder(tf.bool, [args.batch_size], name='edge{0}'.format(i))
                            for i in range(args.max_ast_depth)]

        # projection matrices for output
        self.projection_w = tf.get_variable('projection_w', [self.cell.output_size,
                                                                args.target_vocab_size])
        self.projection_b = tf.get_variable('projection_b', [args.target_vocab_size])

        # setup embedding
        with tf.variable_scope('variational_decoder'):
            embedding = tf.get_variable('embedding', [args.target_vocab_size,
                                                                args.decoder_rnn_units])
            def loop_fn(prev, _):
                prev = tf.nn.xw_plus_b(prev, self.projection_w, self.projection_b)
                prev_symbol = tf.argmax(prev, 1)
                return tf.nn.embedding_lookup(embedding, prev_symbol)

            loop_function = loop_fn if infer else None
            emb_inp = (tf.nn.embedding_lookup(embedding, i) for i in self.nodes)

            # the decoder (modified from tensorflow's seq2seq library to fit tree LSTMs)
            # TODO: update with dynamic decoder (being implemented in tf) once it is released
            with tf.variable_scope('rnn_decoder'):
                self.state = self.initial_state
                self.outputs = []
                prev = None
                for i, inp in enumerate(emb_inp):
                    if loop_function is not None and prev is not None:
                        with tf.variable_scope('loop_function', reuse=True):
                            inp = loop_function(prev, i)
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    with tf.variable_scope('cell1'): # handles CHILD_EDGE
                        output1, state1 = self.cell(inp, self.state)
                    with tf.variable_scope('cell2'): # handles SIBLING_EDGE
                        output2, state2 = self.cell(inp, self.state)
                    output = tf.where(self.edges[i], output1, output2)
                    self.state = tf.where(self.edges[i], state1, state2)
                    self.outputs.append(output)
                    if loop_function is not None:
                        prev = output
