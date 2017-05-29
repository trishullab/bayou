import tensorflow as tf
import numpy as np
import os
import re
import json

from bayou.core.utils import CONFIG_ENCODER, C0, UNK
from bayou.lda.model import LDA


class Evidence(object):

    def init_config(self, evidence, save_dir):
        for attr in CONFIG_ENCODER:
            self.__setattr__(attr, evidence[attr])
        self.load_embedding(save_dir)

    def dump_config(self):
        js = {attr: self.__getattribute__(attr) for attr in CONFIG_ENCODER}
        return js

    @staticmethod
    def read_config(js, save_dir):
        evidences = []
        for evidence in js:
            name = evidence['name']
            if name == 'apicalls':
                e = APICalls()
            elif name == 'types':
                e = Types()
            elif name == 'context':
                e = Context()
            else:
                raise TypeError('Invalid evidence name: {}'.format(name))
            e.init_config(evidence, save_dir)
            evidences.append(e)
        return evidences

    def load_embedding(self, save_dir):
        raise NotImplementedError('load_embedding() has not been implemented')

    def read_data_point(self, program):
        raise NotImplementedError('read_data() has not been implemented')

    def wrangle(self, data):
        raise NotImplementedError('wrangle() has not been implemented')

    def placeholder(self, config):
        raise NotImplementedError('placeholder() has not been implemented')

    def exists(self, inputs):
        raise NotImplementedError('exists() has not been implemented')

    def init_evidence_stdv(self, config):
        raise NotImplementedError('init_evidence_stdv() has not been implemented')

    def encode(self, inputs, config):
        raise NotImplementedError('encode() has not been implemented')

    def evidence_loss(self, psi, encoding):
        raise NotImplementedError('evidence_loss() has not been implemented')


class APICalls(Evidence):

    def load_embedding(self, save_dir):
        embed_save_dir = os.path.join(save_dir, 'embed_apicalls')
        self.lda = LDA(from_file=os.path.join(embed_save_dir, 'model.pkl'))

    def read_data_point(self, program):
        apicalls = program['apicalls'] if 'apicalls' in program else []
        return list(set(apicalls))

    def wrangle(self, data):
        return np.array(self.lda.infer(data), dtype=np.float32)

    def placeholder(self, config):
        return tf.placeholder(tf.float32, [config.batch_size, self.lda.model.n_topics])

    def exists(self, inputs):
        return tf.not_equal(tf.count_nonzero(inputs, axis=1), 0)

    def init_evidence_stdv(self, config):
        with tf.variable_scope('apicalls'):
            self.sigma = tf.get_variable('sigma', [config.latent_size])

    def encode(self, inputs, config):
        with tf.variable_scope('apicalls'):
            encoding = tf.layers.dense(inputs, self.units)
            w = tf.get_variable('w', [self.units, config.latent_size])
            b = tf.get_variable('b', [config.latent_size])
            latent_encoding = tf.nn.xw_plus_b(encoding, w, b) * self.sigma
            return latent_encoding

    def evidence_loss(self, psi, encoding):
        sigma_sq = tf.square(self.sigma)
        loss = 0.5 * (tf.log(2 * np.pi * sigma_sq + 1e-10) + tf.square(encoding - psi) / sigma_sq)
        return self.beta * loss

    @staticmethod
    def from_call(call):
        split = call.split('(')[0].split('.')
        cls, name = split[-2:]
        return [name] if not cls == name else []


class Types(Evidence):

    def load_embedding(self, save_dir):
        embed_save_dir = os.path.join(save_dir, 'embed_types')
        self.lda = LDA(from_file=os.path.join(embed_save_dir, 'model.pkl'))

    def read_data_point(self, program):
        types = program['types'] if 'types' in program else []
        return list(set(types))

    def wrangle(self, data):
        return np.array(self.lda.infer(data), dtype=np.float32)

    def placeholder(self, config):
        return tf.placeholder(tf.float32, [config.batch_size, self.lda.model.n_topics])

    def exists(self, inputs):
        return tf.not_equal(tf.count_nonzero(inputs, axis=1), 0)

    def init_evidence_stdv(self, config):
        with tf.variable_scope('types'):
            self.sigma = tf.get_variable('sigma', [config.latent_size])

    def encode(self, inputs, config):
        with tf.variable_scope('types'):
            encoding = tf.layers.dense(inputs, self.units)
            w = tf.get_variable('w', [self.units, config.latent_size])
            b = tf.get_variable('b', [config.latent_size])
            latent_encoding = tf.nn.xw_plus_b(encoding, w, b) * self.sigma
            return latent_encoding

    def evidence_loss(self, psi, encoding):
        sigma_sq = tf.square(self.sigma)
        loss = 0.5 * (tf.log(2 * np.pi * sigma_sq + 1e-10) + tf.square(encoding - psi) / sigma_sq)
        return self.beta * loss

    @staticmethod
    def from_call(call):
        split = list(reversed([q for q in call.split('(')[0].split('.')[:-1] if q[0].isupper()]))
        return [split[1], split[0]] if len(split) > 1 else [split[0]]


class Context(Evidence):

    def load_embedding(self, save_dir):
        embed_save_dir = os.path.join(save_dir, 'embed_context')
        self.lda = LDA(from_file=os.path.join(embed_save_dir, 'model.pkl'))

    def read_data_point(self, program):
        context = program['context'] if 'context' in program else []
        return list(set(context))

    def wrangle(self, data):
        return np.array(self.lda.infer(data), dtype=np.float32)

    def placeholder(self, config):
        return tf.placeholder(tf.float32, [config.batch_size, self.lda.model.n_topics])

    def exists(self, inputs):
        return tf.not_equal(tf.count_nonzero(inputs, axis=1), 0)

    def init_evidence_stdv(self, config):
        with tf.variable_scope('context'):
            self.sigma = tf.get_variable('sigma', [config.latent_size])

    def encode(self, inputs, config):
        with tf.variable_scope('context'):
            encoding = tf.layers.dense(inputs, self.units)
            w = tf.get_variable('w', [self.units, config.latent_size])
            b = tf.get_variable('b', [config.latent_size])
            latent_encoding = tf.nn.xw_plus_b(encoding, w, b) * self.sigma
            return latent_encoding

    def evidence_loss(self, psi, encoding):
        sigma_sq = tf.square(self.sigma)
        loss = 0.5 * (tf.log(2 * np.pi * sigma_sq + 1e-10) + tf.square(encoding - psi) / sigma_sq)
        return self.beta * loss

    @staticmethod
    def from_call(call):
        args = call.split('(')[1].split(')')[0].split(',')
        args = [arg.split('.')[-1] for arg in args]
        args = [re.sub('<.*', r'', arg) for arg in args]  # remove generics
        args = [re.sub('\[\]', r'', arg) for arg in args]  # remove array type
        return [arg for arg in args if not arg == '']


# TODO: handle Javadoc with word2vec
class Javadoc(Evidence):

    def read_data_point(self, program, infer=False):
        javadoc = program['javadoc'] if 'javadoc' in program else None
        if not javadoc:
            javadoc = UNK
        try:  # do not consider non-ASCII javadoc
            javadoc.encode('ascii')
        except UnicodeEncodeError:
            javadoc = UNK
        javadoc = javadoc.split()
        return javadoc

    def set_dicts(self, data):
        if self.pretrained_embed:
            save_dir = os.path.join(self.save_dir, 'embed_' + self.name)
            with open(os.path.join(save_dir, 'config.json')) as f:
                js = json.load(f)
            self.chars = js['chars']
        else:
            self.chars = [C0] + list(set([w for point in data for w in point]))
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.vocab_size = len(self.vocab)


