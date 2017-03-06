import tensorflow as tf
from tensorflow.contrib import rnn
from tensorflow.contrib import legacy_seq2seq

class Encoder(object):
    def __init__(self, args):
        if args.cell == 'lstm':
            cell = rnn.BasicLSTMCell(args.encoder_rnn_size, state_is_tuple=False)
        else:
            cell = rnn.BasicRNNCell(args.encoder_rnn_size)

        self.seq = [tf.placeholder(tf.int32, [args.batch_size, args.max_seq_length, 1], 
                        name='seq{0}'.format(i)) for i in range(args.max_seqs)]
        self.seq_length = [tf.placeholder(tf.int32, [args.batch_size],
                            name='seq_length{0}'.format(i)) for i in range(args.max_seqs)]

        with tf.variable_scope('encoder'):
            # dynamic RNN
            self.cell = rnn.EmbeddingWrapper(cell, 
                                    embedding_classes=args.input_vocab_size,
                                    embedding_size=args.encoder_rnn_size)
            self.cell_init = self.cell.zero_state(args.batch_size, tf.float32)
            encodings = []
            for i, (seq, seq_length) in enumerate(zip(self.seq, self.seq_length)):
                if i > 0:
                    tf.get_variable_scope().reuse_variables()
                _, encoding_seq = tf.nn.dynamic_rnn(self.cell, seq,
                                        sequence_length=seq_length,
                                        initial_state=self.cell_init,
                                        dtype=tf.float32)
                encodings.append(encoding_seq)
            self.encoding = tf.reshape(tf.stack(encodings, axis=1), [args.batch_size,
                                args.decoder_rnn_size * (2 if args.cell == 'lstm' else 1)])

class Decoder(object):
    def __init__(self, args, initial_state, infer=False):
        if args.cell == 'lstm':
            cell = rnn.BasicLSTMCell(args.decoder_rnn_size, state_is_tuple=False)
        else:
            cell = rnn.BasicRNNCell(args.decoder_rnn_size)

        # placeholders
        self.initial_state = initial_state
        self.nodes = [tf.placeholder(tf.int32, [args.batch_size], name='node{0}'.format(i))
                            for i in range(args.max_ast_depth)]
        self.edges = [tf.placeholder(tf.bool, [args.batch_size], name='edge{0}'.format(i))
                            for i in range(args.max_ast_depth)]

        # projection matrices for output
        self.projection_w = tf.get_variable('projection_w', [args.decoder_rnn_size,
                                                                    args.target_vocab_size])
        self.projection_b = tf.get_variable('projection_b', [args.target_vocab_size])

        # setup embedding
        with tf.variable_scope('decoder'):
            embedding = tf.get_variable('embedding', [args.target_vocab_size,
                                                                args.decoder_rnn_size])
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
                        output1, state1 = cell(inp, self.state)
                    with tf.variable_scope('cell2'): # handles SIBLING_EDGE
                        output2, state2 = cell(inp, self.state)
                    output = tf.where(self.edges[i], output1, output2)
                    self.state = tf.where(self.edges[i], state1, state2)
                    self.outputs.append(output)
                    if loop_function is not None:
                        prev = output
