import tensorflow as tf

class VariationalEncoder(object):
    def __init__(self, args):

        self.seq = [tf.placeholder(tf.int32, [args.batch_size, args.max_seq_length, 1], 
                        name='seq{0}'.format(i)) for i in range(args.max_seqs)]
        self.seq_length = [tf.placeholder(tf.int32, [args.batch_size],
                            name='seq_length{0}'.format(i)) for i in range(args.max_seqs)]

        with tf.variable_scope('variational_encoder_mean'):
            # mean encoder
            self.cell_mean = tf.nn.rnn_cell.EmbeddingWrapper(args.cell_fn,
                                    embedding_classes=args.input_vocab_size,
                                    embedding_size=args.rnn_size)
            self.cell_mean_init = self.cell_mean.zero_state(args.batch_size, tf.float32)
            means = []
            with tf.variable_scope('mean_rnn'):
                for i, (seq, seq_length) in enumerate(zip(self.seq, self.seq_length)):
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    _, mean = tf.nn.dynamic_rnn(self.cell_mean, seq,
                                            sequence_length=seq_length,
                                            initial_state=self.cell_mean_init,
                                            dtype=tf.float32)
                    means.append(mean)
            means = tf.pack(means)
            sum_of_means = tf.reduce_sum(means, axis=0)
            num_non_zero_means = tf.count_nonzero(means, axis=0, dtype=tf.float32)
            mean_means = tf.div(sum_of_means, num_non_zero_means)
            latent_w_mean = tf.get_variable('latent_w_mean', [self.cell_mean.state_size,
                                                                args.latent_size])
            latent_b_mean = tf.get_variable('latent_b_mean', [args.latent_size])
            self.psi_mean = tf.matmul(mean_means, latent_w_mean) + latent_b_mean

        with tf.variable_scope('variational_encoder_stdv'):
            # standard deviation encoder
            self.cell_stdv = tf.nn.rnn_cell.EmbeddingWrapper(args.cell_fn,
                                        embedding_classes=args.input_vocab_size,
                                        embedding_size=args.rnn_size)
            self.cell_stdv_init = self.cell_stdv.zero_state(args.batch_size, tf.float32)
            stdvs = []
            with tf.variable_scope('stdv_rnn'):
                for i, (seq, seq_length) in enumerate(zip(self.seq, self.seq_length)):
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    _, stdv = tf.nn.dynamic_rnn(self.cell_stdv, seq,
                                            sequence_length=seq_length,
                                            initial_state=self.cell_stdv_init,
                                            dtype=tf.float32)
                    stdvs.append(stdv)
            stdvs = tf.pack(stdvs)
            sum_of_stdvs = tf.reduce_sum(stdvs, axis=0)
            num_non_zero_stdvs = tf.count_nonzero(stdvs, axis=0, dtype=tf.float32)
            mean_stdvs = tf.div(sum_of_stdvs, num_non_zero_stdvs)
            latent_w_stdv = tf.get_variable('latent_w_stdv', [self.cell_stdv.state_size,
                                                                args.latent_size])
            latent_b_stdv = tf.get_variable('latent_b_stdv', [args.latent_size])
            self.psi_stdv = tf.matmul(mean_stdvs, latent_w_stdv) + latent_b_stdv

class VariationalDecoder(object):
    def __init__(self, args, initial_state, infer=False):

        # placeholders
        self.initial_state = initial_state
        self.nodes = [tf.placeholder(tf.int32, [args.batch_size], name='node{0}'.format(i))
                            for i in range(args.max_ast_depth)]
        self.edges = [tf.placeholder(tf.bool, [args.batch_size], name='edge{0}'.format(i))
                            for i in range(args.max_ast_depth)]

        # projection matrices for output
        self.projection_w = tf.get_variable('projection_w', [args.cell_fn.output_size,
                                                                args.target_vocab_size])
        self.projection_b = tf.get_variable('projection_b', [args.target_vocab_size])

        # setup embedding
        with tf.variable_scope('variational_decoder'):
            embedding = tf.get_variable('embedding', [args.target_vocab_size, args.rnn_size])
            loop_function = tf.nn.seq2seq._extract_argmax_and_embed(embedding, 
                                    (self.projection_w, self.projection_b)) if infer else None
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
                        output1, state1 = args.cell_fn(inp, self.state)
                    with tf.variable_scope('cell2'): # handles SIBLING_EDGE
                        output2, state2 = args.cell_fn(inp, self.state)
                    output = tf.where(self.edges[i], output1, output2)
                    self.state = tf.where(self.edges[i], state1, state2)
                    self.outputs.append(output)
                    if loop_function is not None:
                        prev = output
