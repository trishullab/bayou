import tensorflow as tf
from tensorflow.contrib import rnn
import numpy as np

from variational.utils import CONFIG_ENCODER, CONFIG_CHARS_VOCAB, C0
from variational.utils import length, sub_sequences

class Evidence(object):

    def init_config(self, evidence, chars_vocab):
        attrs = CONFIG_ENCODER + (CONFIG_CHARS_VOCAB if chars_vocab else [])
        for attr in attrs:
            self.__setattr__(attr, evidence[attr])

    def dump_config(self):
        attrs = CONFIG_ENCODER + CONFIG_CHARS_VOCAB
        js = { attr: self.__getattribute__(attr) for attr in attrs }
        return js

    @staticmethod
    def read_config(js, chars_vocab):
        evidences = []
        for evidence in js:
            name = evidence['name']
            if name == 'sequences':
                e = Sequences()
            elif name == 'keywords':
                e = Keywords()
            else:
                raise TypeError('Invalid evidence name: {}'.format(name))
            e.init_config(evidence, chars_vocab)
            evidences.append(e)
        return evidences

    def read_data(self, program):
        raise NotImplementedError('read_data() has not been implemented')

    def set_vocab_chars(self, data):
        raise NotImplementedError('set_vocab_chars() has not been implemented')

    def wrangle(self, data, sz):
        raise NotImplementedError('wrangle() has not been implemented')

    def placeholder(self, config):
        return [tf.placeholder(tf.int32, [config.batch_size, self.max_length, 1], 
                            name='{}{}'.format(self.name, i)) for i in range(self.max_num)]

    def reshape(self, data_point):
        return [data_point[:, i, :, :] for i in range(self.max_num)]

    def exists_tiled(self, inputs):
        zero = tf.constant(0, dtype=tf.int32)
        lengths = [length(inp) for inp in inputs]
        exists = [tf.not_equal(l, zero) for l in lengths] * self.tile
        return exists

    def encode(self, inputs, config):
        cell = rnn.BasicLSTMCell(self.rnn_units, state_is_tuple=False) if config.cell == 'lstm' \
               else rnn.BasicRNNCell(self.rnn_units)

        encodings = []
        with tf.variable_scope(self.name):
            cell = rnn.EmbeddingWrapper(cell,
                                embedding_classes=self.vocab_size,
                                embedding_size=self.rnn_units)
            self.cell_init = cell.zero_state(config.batch_size, tf.float32)
            w = tf.get_variable('w', [cell.state_size, config.latent_size])
            b = tf.get_variable('b', [config.latent_size])

            for i, inp in enumerate(inputs):
                if i > 0:
                    tf.get_variable_scope().reuse_variables()
                _, encoding = tf.nn.dynamic_rnn(cell, inp,
                                        sequence_length=length(inp),
                                        initial_state=self.cell_init,
                                        dtype=tf.float32)
                encodings.append(tf.nn.xw_plus_b(encoding, w, b))

        encodings = encodings * self.tile
        return encodings

class Sequences(Evidence):

    def read_data(self, program):
        sequences = sub_sequences([sequence['calls'] for sequence in program['sequences']])
        assert len(sequences) <= self.max_num
        assert all([len(sequence) <= self.max_length for sequence in sequences])
        return sequences

    def set_vocab_chars(self, data):
        self.chars = [C0] + list(set([call for point in data for seq in point for call in seq]))
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.vocab_size = len(self.vocab)

    def wrangle(self, data, sz):
        sequences = np.zeros((sz, self.max_num, self.max_length, 1), dtype=np.int32)
        for i, set_of_seqs in enumerate(data):
            for j, seq in enumerate(set_of_seqs):
                sequences[i, j, :len(seq), 0] = list(map(self.vocab.get, seq))
        return sequences

class Keywords(Evidence):

    def read_data(self, program):
        keywords = program['keywords']
        assert len(keywords) <= self.max_num
        return keywords

    def set_vocab_chars(self, data):
        self.chars = [C0] + list(set([kw for point in data for kw in point]))
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.vocab_size = len(self.vocab)

    def wrangle(self, data, sz):
        keywords = np.zeros((sz, self.max_num, 1, 1), dtype=np.int32)
        for i, kws in enumerate(data):
            keywords[i, :len(kws), 0, 0] = list(map(self.vocab.get, kws))
        return keywords
