import tensorflow as tf
from tensorflow.contrib import rnn
from itertools import chain
import numpy as np
import re

from bayou.utils import CONFIG_ENCODER, CONFIG_CHARS_VOCAB, C0
from bayou.utils import length

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

    def read_data(self, program, infer=False):
        raise NotImplementedError('read_data() has not been implemented')

    def set_vocab_chars(self, data):
        raise NotImplementedError('set_vocab_chars() has not been implemented')

    def wrangle(self, data):
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

    def read_data(self, program, infer=False):
        sequences = self.sub_sequences([sequence['calls'] for sequence in program['sequences']])
        assert len(sequences) <= self.max_num
        assert all([len(sequence) <= self.max_length for sequence in sequences])
        return sequences

    def set_vocab_chars(self, data):
        self.chars = [C0] + list(set([call for point in data for seq in point for call in seq]))
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.vocab_size = len(self.vocab)

    def wrangle(self, data):
        sequences = np.zeros((len(data), self.max_num, self.max_length, 1), dtype=np.int32)
        for i, set_of_seqs in enumerate(data):
            for j, seq in enumerate(set_of_seqs):
                sequences[i, j, :len(seq), 0] = list(map(self.vocab.get, seq))
        return sequences

    def sub_sequences(self, sequences):
        ret = []
        for sequence in sequences:
            cuts = [sequence[:i] for i in range(1, len(sequence)+1)]
            for s in cuts:
                if s not in ret:
                    ret.append(s)
        return ret

class Keywords(Evidence):

    def read_data(self, program, infer=False):
        keywords = program['keywords']
        if infer:
            if type(keywords) is str:
                keywords = keywords.split()
            keywords += self.keywords_from_sequences(program['sequences'])
            keywords = list(set([k for k in keywords if k in self.vocab]))
        assert len(keywords) <= self.max_num
        return keywords

    def set_vocab_chars(self, data):
        self.chars = [C0] + list(set([kw for point in data for kw in point]))
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.vocab_size = len(self.vocab)

    def wrangle(self, data):
        keywords = np.zeros((len(data), self.max_num, 1, 1), dtype=np.int32)
        for i, kws in enumerate(data):
            keywords[i, :len(kws), 0, 0] = list(map(self.vocab.get, kws))
        return keywords

    def keywords_from_sequences(self, sequences):

        def split_camel(s):
            s1 = re.sub('(.)([A-Z][a-z]+)', r'\1#\2', s) # LC followed by UC
            s1 = re.sub('([a-z0-9])([A-Z])', r'\1#\2', s1) # UC followed by LC
            return s1.split('#')

        def get_name(call):
            q = call.split('(')[0].split('.')
            cls, name = q[-2], q[-1]
            return cls + '#' + name

        calls = set([get_name(call) for sequence in sequences for call in sequence['calls']])
        keywords = list(chain.from_iterable([split_camel(call) for call in calls]))
        return list(set([kw.lower() for kw in keywords if not kw == '']))

