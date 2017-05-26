import tensorflow as tf
from tensorflow.contrib import rnn
from itertools import chain


class BayesianEncoder(object):
    def __init__(self, config):

        psi = []
        self.inputs = [ev.placeholder(config) for ev in config.evidence]
        exists = [ev.exists(i) for ev, i in zip(config.evidence, self.inputs)]
        zeros = tf.zeros([config.batch_size, config.latent_size], dtype=tf.float32)
        for ev in config.evidence:
            ev.init_evidence_stdv(config)
        input_encodings = []

        # To compute Psi
        for scope in ['mean', 'stdv']:
            with tf.variable_scope(scope):
                # 1. compute encoding
                encodings = [ev.encode(i, config) for ev, i in zip(config.evidence, self.inputs)]

                # 2. pick only encodings from valid inputs that exist, otherwise pick zero encoding
                encodings = [tf.where(exist, enc, zeros) for exist, enc in zip(exists, encodings)]
                input_encodings.append(encodings)

                # 3. tile the encodings according to each evidence type
                encodings = [[enc] * ev.tile for ev, enc in zip(config.evidence, encodings)]
                encodings = tf.stack(list(chain.from_iterable(encodings)))

                # 4. compute the mean of non-zero encodings
                num_nonzero = tf.count_nonzero(encodings, axis=0, dtype=tf.float32)
                sum_encodings = tf.reduce_sum(encodings, axis=0)
                mean_encodings = sum_encodings / num_nonzero
                psi.append(mean_encodings)

        self.psi_mean, self.psi_stdv = psi

        # To compute input encoding, f(\theta), for each type of evidence
        self.input_encodings = [mean + stdv for mean, stdv in zip(*input_encodings)]
        assert len(self.input_encodings) == len(config.evidence)


class BayesianDecoder(object):
    def __init__(self, config, initial_state, infer=False):

        self.cell1 = rnn.GRUCell(config.decoder.units)
        self.cell2 = rnn.GRUCell(config.decoder.units)

        # placeholders
        self.initial_state = initial_state
        self.nodes = [tf.placeholder(tf.int32, [config.batch_size], name='node{0}'.format(i))
                      for i in range(config.decoder.max_ast_depth)]
        self.edges = [tf.placeholder(tf.bool, [config.batch_size], name='edge{0}'.format(i))
                      for i in range(config.decoder.max_ast_depth)]

        # projection matrices for output
        self.projection_w = tf.get_variable('projection_w', [self.cell1.output_size,
                                                             config.decoder.vocab_size])
        self.projection_b = tf.get_variable('projection_b', [config.decoder.vocab_size])

        # setup embedding
        with tf.variable_scope('decoder'):
            emb = tf.get_variable('emb', [config.decoder.vocab_size, config.decoder.units])

            def loop_fn(prev, _):
                prev = tf.nn.xw_plus_b(prev, self.projection_w, self.projection_b)
                prev_symbol = tf.argmax(prev, 1)
                return tf.nn.embedding_lookup(emb, prev_symbol)

            loop_function = loop_fn if infer else None
            emb_inp = (tf.nn.embedding_lookup(emb, i) for i in self.nodes)

            # the decoder (modified from tensorflow's seq2seq library to fit tree RNNs)
            # TODO: update with dynamic decoder (being implemented in tf) once it is released
            with tf.variable_scope('rnn'):
                self.state = self.initial_state
                self.outputs = []
                prev = None
                for i, inp in enumerate(emb_inp):
                    if loop_function is not None and prev is not None:
                        with tf.variable_scope('loop_function', reuse=True):
                            inp = loop_function(prev, i)
                    if i > 0:
                        tf.get_variable_scope().reuse_variables()
                    with tf.variable_scope('cell1'):  # handles CHILD_EDGE
                        output1, state1 = self.cell1(inp, self.state)
                    with tf.variable_scope('cell2'):  # handles SIBLING_EDGE
                        output2, state2 = self.cell2(inp, self.state)
                    output = tf.where(self.edges[i], output1, output2)
                    self.state = tf.where(self.edges[i], state1, state2)
                    self.outputs.append(output)
                    if loop_function is not None:
                        prev = output
