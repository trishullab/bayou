import tensorflow as tf

class VariationalEncoder(object):
    def __init__(self, args):
        # to handle mean and standard deviation
        cell_fn = tf.nn.rnn_cell.BasicLSTMCell if args.cell == 'lstm' \
                  else tf.nn.rnn_cell.BasicRNNCell 
        cell1 = cell_fn(args.rnn_size)
        cell2 = cell_fn(args.rnn_size)

        self.seq = tf.placeholder(tf.int32, [args.batch_size, args.max_seq_length, 1], name='seq')
        self.seq_length = tf.placeholder(tf.int32, [args.batch_size], name='seq_length')

        with tf.variable_scope('variational_encoder'):
            # mean RNN
            with tf.variable_scope('mean'):
                self.cell_mean = tf.nn.rnn_cell.EmbeddingWrapper(cell1, 
                                        embedding_classes=args.input_vocab_size,
                                        embedding_size=args.rnn_size)
                self.cell_mean_init = self.cell_mean.zero_state(args.batch_size, tf.float32)
                _, psi_mean = tf.nn.dynamic_rnn(self.cell_mean, self.seq,
                                        sequence_length=self.seq_length,
                                        initial_state=self.cell_mean_init,
                                        dtype=tf.float32)
                latent_w_mean = tf.get_variable('latent_w_mean', [args.rnn_size, args.latent_size])
                latent_b_mean = tf.get_variable('latent_b_mean', [args.latent_size])
                self.psi_mean = tf.matmul(psi_mean, latent_w_mean) + latent_b_mean

            # standard deviation RNN
            with tf.variable_scope('stdv'):
                self.cell_stdv = tf.nn.rnn_cell.EmbeddingWrapper(cell2, 
                                            embedding_classes=args.input_vocab_size,
                                            embedding_size=args.rnn_size)
                self.cell_stdv_init = self.cell_stdv.zero_state(args.batch_size, tf.float32)
                _, psi_stdv = tf.nn.dynamic_rnn(self.cell_stdv, self.seq,
                                        sequence_length=self.seq_length,
                                        initial_state=self.cell_stdv_init,
                                        dtype=tf.float32)
                latent_w_stdv = tf.get_variable('latent_w_stdv', [args.rnn_size, args.latent_size])
                latent_b_stdv = tf.get_variable('latent_b_stdv', [args.latent_size])
                self.psi_stdv = tf.matmul(psi_stdv, latent_w_stdv) + latent_b_stdv

class VariationalDecoder(object):
    def __init__(self, args, initial_state, infer=False):
        # to handle different types of edges (CHILD_EDGE, SIBLING_EDGE)
        cell_fn = tf.nn.rnn_cell.BasicLSTMCell if args.cell == 'lstm' \
                  else tf.nn.rnn_cell.BasicRNNCell 
        cell1 = cell_fn(args.rnn_size)
        cell2 = cell_fn(args.rnn_size)

        # placeholders
        self.initial_state = initial_state
        self.nodes = [tf.placeholder(tf.int32, [args.batch_size], name='node{0}'.format(i))
                            for i in range(args.max_ast_depth)]
        self.edges = [tf.placeholder(tf.bool, [args.batch_size], name='edge{0}'.format(i))
                            for i in range(args.max_ast_depth)]

        # projection matrices for output
        self.projection_w = tf.get_variable('projection_w', [args.rnn_size, args.target_vocab_size])
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
                        output1, state1 = cell1(inp, self.state)
                    with tf.variable_scope('cell2'): # handles SIBLING_EDGE
                        output2, state2 = cell2(inp, self.state)
                    output = tf.where(self.edges[i], output1, output2)
                    self.state = tf.where(self.edges[i], state1, state2)
                    self.outputs.append(output)
                    if loop_function is not None:
                        prev = output
