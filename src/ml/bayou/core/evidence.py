import tensorflow as tf
from tensorflow.contrib import rnn
from itertools import chain
import numpy as np
import os
import json
import re

from bayou.core.utils import CONFIG_ENCODER, CONFIG_CHARS_VOCAB, C0, UNK
from bayou.core.utils import length
from bayou.core.cells import PretrainedEmbeddingWrapper


class Evidence(object):

    def init_config(self, evidence, chars_vocab, save_dir):
        attrs = CONFIG_ENCODER + (CONFIG_CHARS_VOCAB if chars_vocab else [])
        self.save_dir = save_dir
        for attr in attrs:
            self.__setattr__(attr, evidence[attr])

    def dump_config(self):
        attrs = CONFIG_ENCODER + CONFIG_CHARS_VOCAB
        js = { attr: self.__getattribute__(attr) for attr in attrs }
        return js

    @staticmethod
    def read_config(js, chars_vocab, save_dir):
        evidences = []
        for evidence in js:
            name = evidence['name']
            if name == 'keywords':
                e = Keywords()
            elif name == 'javadoc':
                e = Javadoc()
            elif name == 'types':
                e = Types()
            else:
                raise TypeError('Invalid evidence name: {}'.format(name))
            e.init_config(evidence, chars_vocab, save_dir)
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

    def load_pretrained_embeddings(self, sess, save_dir):
        if self.pretrained_embed:
            self.pretrained_embeddings.load_from(sess, save_dir)

    def encode(self, inputs, config):
        cell = rnn.BasicLSTMCell(self.rnn_units, state_is_tuple=False) if config.cell == 'lstm' \
               else rnn.BasicRNNCell(self.rnn_units)

        encodings = []
        with tf.variable_scope(self.name):
            if self.pretrained_embed:
                cell = self.pretrained_embeddings = PretrainedEmbeddingWrapper(cell, self)
            else:
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
                latent_encoding = tf.nn.xw_plus_b(encoding, w, b)
                encodings.append(latent_encoding)

        encodings = encodings * self.tile
        return encodings


class Keywords(Evidence):

    def read_data(self, program, infer=False):
        keywords = program['keywords'] if 'keywords' in program else []
        if type(keywords) is str:
            keywords = keywords.split()
        keywords += Keywords.keywords_from_sequences(program['sequences'])
        if infer:
            keywords = list(set([k for k in keywords if k in self.vocab]))
        else:
            keywords = list(set(keywords))
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

    @staticmethod
    def split_camel(s):
        s1 = re.sub('(.)([A-Z][a-z]+)', r'\1#\2', s) # UC followed by LC
        s1 = re.sub('([a-z0-9])([A-Z])', r'\1#\2', s1) # LC followed by UC
        return s1.split('#')

    @staticmethod
    def get_name(call):
        q = call.split('(')[0].split('.')
        cls, name = q[-2], q[-1]
        return cls + '#' + name

    @staticmethod
    def keywords_from_sequences(sequences):
        calls = set([Keywords.get_name(call) for sequence in sequences \
                for call in sequence['calls']])
        keywords = list(chain.from_iterable([Keywords.split_camel(call) for call in calls]))
        return list(set([kw.lower() for kw in keywords if not kw == '']))


class Javadoc(Evidence):

    def read_data(self, program, infer=False):
        javadoc = program['javadoc'] if 'javadoc' in program else None
        if not javadoc:
            javadoc = UNK
        try: # do not consider non-ASCII javadoc
            javadoc.encode('ascii')
        except UnicodeEncodeError:
            javadoc = UNK
        javadoc = javadoc.split()
        assert len(javadoc) <= self.max_length
        return javadoc

    def set_vocab_chars(self, data):
        if self.pretrained_embed:
            save_dir = os.path.join(self.save_dir, 'embed_' + self.name)
            with open(os.path.join(save_dir, 'config.json')) as f:
                js = json.load(f)
            self.chars = js['chars']
        else:
            self.chars = [C0] + list(set([w for point in data for w in point]))
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.vocab_size = len(self.vocab)

    def wrangle(self, data):
        javadoc = np.zeros((len(data), 1, self.max_length, 1), dtype=np.int32)
        for i, jd in enumerate(data):
            javadoc[i, 0, :len(jd), 0] = list(map(self.vocab.get, jd))
        return javadoc


class Types(Evidence):

    def read_data(self, program, infer=False):
        types = program['types'] if 'types' in program else []
        types += self.types_from_sequences(program['sequences'])
        if infer:
            types = list(set([t for t in types if t in self.vocab]))
        else:
            types = list(set(types))
        assert len(types) <= self.max_num
        return types

    def set_vocab_chars(self, data):
        self.chars = [C0] + list(set([t for point in data for t in point]))
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.vocab_size = len(self.vocab)

    def wrangle(self, data):
        types = np.zeros((len(data), self.max_num, 1, 1), dtype=np.int32)
        for i, t in enumerate(data):
            types[i, :len(t), 0, 0] = list(map(self.vocab.get, t))
        return types

    def types_from_sequences(self, sequences):

        def remove_generics(s):
            s = s.split('#')
            return [re.sub('\<.*', r'', c) for c in s]

        def get_class(call):
            qUC = reversed([q for q in call.split('(')[0].split('.') if q[0].isupper()])
            inner = next(qUC)
            try:
                outer = next(qUC)
            except StopIteration:
                outer = ''
            return outer + '#' + inner

        calls = set([get_class(call) for sequence in sequences for call in sequence['calls']])
        types = list(chain.from_iterable([remove_generics(call) for call in calls]))
        return list(set([t for t in types if not t == '']))

