# Copyright 2017 Rice University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import tensorflow as tf
import numpy as np
import os
import re
import json
from itertools import chain

from bayou.models.core.utils import CONFIG_ENCODER, C0, UNK
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
            elif name == 'keywords':
                e = Keywords()
            # javadoc_(No.)
            elif name[:7] == 'javadoc':
                order = name[-1:]
                e = Javadoc(order, evidence['max_length'], evidence['filter_sizes'], evidence['num_filters'])
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

    def init_sigma(self, config):
        raise NotImplementedError('init_sigma() has not been implemented')

    def encode(self, inputs, config):
        raise NotImplementedError('encode() has not been implemented')

    def evidence_loss(self, psi, encoding, config):
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
        return tf.placeholder(tf.float32, [config.batch_size, self.lda.model.n_components])

    def exists(self, inputs):
        return tf.not_equal(tf.count_nonzero(inputs, axis=1), 0)

    def init_sigma(self, config):
        with tf.variable_scope('apicalls'):
            self.sigma = tf.get_variable('sigma', [])

    def encode(self, inputs, config):
        with tf.variable_scope('apicalls'):
            encoding = tf.layers.dense(inputs, self.units, activation=tf.nn.tanh)
            for i in range(self.num_layers - 1):
                encoding = tf.layers.dense(encoding, self.units, activation=tf.nn.tanh)
            w = tf.get_variable('w', [self.units, config.latent_size])
            b = tf.get_variable('b', [config.latent_size])
            latent_encoding = tf.nn.xw_plus_b(encoding, w, b)
            return latent_encoding

    def evidence_loss(self, psi, encoding, config):
        sigma_sq = tf.square(self.sigma)
        loss = 0.5 * (config.latent_size * tf.log(2 * np.pi * sigma_sq + 1e-10)
                      + tf.square(encoding - psi) / sigma_sq)
        return loss

    @staticmethod
    def from_call(callnode):
        call = callnode['_call']
        call = re.sub('^\$.*\$', '', call)  # get rid of predicates
        split = call.split('(')[0].split('.')
        cls, name = split[-2:]
        cls = cls.split('<')[0]  # class name might be generic but method name is never
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
        return tf.placeholder(tf.float32, [config.batch_size, self.lda.model.n_components])

    def exists(self, inputs):
        return tf.not_equal(tf.count_nonzero(inputs, axis=1), 0)

    def init_sigma(self, config):
        with tf.variable_scope('types'):
            self.sigma = tf.get_variable('sigma', [])

    def encode(self, inputs, config):
        with tf.variable_scope('types'):
            encoding = tf.layers.dense(inputs, self.units, activation=tf.nn.tanh)
            for i in range(self.num_layers - 1):
                encoding = tf.layers.dense(encoding, self.units, activation=tf.nn.tanh)
            w = tf.get_variable('w', [self.units, config.latent_size])
            b = tf.get_variable('b', [config.latent_size])
            latent_encoding = tf.nn.xw_plus_b(encoding, w, b)
            return latent_encoding

    def evidence_loss(self, psi, encoding, config):
        sigma_sq = tf.square(self.sigma)
        loss = 0.5 * (config.latent_size * tf.log(2 * np.pi * sigma_sq + 1e-10)
                      + tf.square(encoding - psi) / sigma_sq)
        return loss

    @staticmethod
    def from_call(callnode):
        call = callnode['_call']
        call = re.sub('^\$.*\$', '', call)  # get rid of predicates
        split = list(reversed([q for q in call.split('(')[0].split('.')[:-1] if q[0].isupper()]))
        types = [split[1], split[0]] if len(split) > 1 else [split[0]]
        types = [re.sub('<.*', r'', t) for t in types]  # ignore generic types in evidence

        args = call.split('(')[1].split(')')[0].split(',')
        args = [arg.split('.')[-1] for arg in args]
        args = [re.sub('<.*', r'', arg) for arg in args]  # remove generics
        args = [re.sub('\[\]', r'', arg) for arg in args]  # remove array type
        types_args = [arg for arg in args if not arg == '' and not arg.startswith('Tau_')]

        if '_throws' in callnode:
            throws = [throw.split('.')[-1] for throw in callnode['_throws']]
            throws = [re.sub('<.*', r'', throw) for throw in throws]  # remove generics
            throws = [re.sub('\[\]', r'', throw) for throw in throws]  # remove array type
            throws = [throw for throw in throws if not throw.startswith('Tau_')]
        else:
            throws = []

        if '_returns' in callnode:
            ret = callnode['_returns'].split('.')[-1]
            ret = re.sub('<.*', r'', ret)  # remove generics
            ret = re.sub('\[\]', r'', ret)  # remove array type
            returns = [] if ret.startswith('Tau_') or ret == 'void' else [ret]
        else:
            returns = []

        return types + types_args + throws + returns


class Keywords(Evidence):

    STOP_WORDS = {  # CoreNLP English stop words
        "'ll", "'s", "'m", "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between",
        "both", "but", "by", "can", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does",
        "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had",
        "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her",
        "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll",
        "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me",
        "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only",
        "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she",
        "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's",
        "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they",
        "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under",
        "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't",
        "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom",
        "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've",
        "your", "yours", "yourself", "yourselves", "return", "arent", "cant", "couldnt", "didnt", "doesnt",
        "dont", "hadnt", "hasnt", "havent", "hes", "heres", "hows", "im", "isnt", "its", "lets", "mustnt",
        "shant", "shes", "shouldnt", "thats", "theres", "theyll", "theyre", "theyve", "wasnt", "were",
        "werent", "whats", "whens", "wheres", "whos", "whys", "wont", "wouldnt", "youd", "youll", "youre",
        "youve"
    }

    def load_embedding(self, save_dir):
        embed_save_dir = os.path.join(save_dir, 'embed_keywords')
        self.lda = LDA(from_file=os.path.join(embed_save_dir, 'model.pkl'))

    def read_data_point(self, program):
        keywords = program['keywords'] if 'keywords' in program else []
        return list(set(keywords))

    def wrangle(self, data):
        return np.array(self.lda.infer(data), dtype=np.float32)

    def placeholder(self, config):
        return tf.placeholder(tf.float32, [config.batch_size, self.lda.model.n_components])

    def exists(self, inputs):
        return tf.not_equal(tf.count_nonzero(inputs, axis=1), 0)

    def init_sigma(self, config):
        with tf.variable_scope('keywords'):
            self.sigma = tf.get_variable('sigma', [])

    def encode(self, inputs, config):
        with tf.variable_scope('keywords'):
            encoding = tf.layers.dense(inputs, self.units, activation=tf.nn.tanh)
            for i in range(self.num_layers - 1):
                encoding = tf.layers.dense(encoding, self.units, activation=tf.nn.tanh)
            w = tf.get_variable('w', [self.units, config.latent_size])
            b = tf.get_variable('b', [config.latent_size])
            latent_encoding = tf.nn.xw_plus_b(encoding, w, b)
            return latent_encoding

    def evidence_loss(self, psi, encoding, config):
        sigma_sq = tf.square(self.sigma)
        loss = 0.5 * (config.latent_size * tf.log(2 * np.pi * sigma_sq + 1e-10)
                      + tf.square(encoding - psi) / sigma_sq)
        return loss

    @staticmethod
    def split_camel(s):
        s = re.sub('(.)([A-Z][a-z]+)', r'\1#\2', s)  # UC followed by LC
        s = re.sub('([a-z0-9])([A-Z])', r'\1#\2', s)  # LC followed by UC
        return s.split('#')

    @staticmethod
    def from_call(callnode):
        call = callnode['_call']
        call = re.sub('^\$.*\$', '', call)  # get rid of predicates
        qualified = call.split('(')[0]
        qualified = re.sub('<.*>', '', qualified).split('.')  # remove generics for keywords

        # add qualified names (java, util, xml, etc.), API calls and types
        keywords = list(chain.from_iterable([Keywords.split_camel(s) for s in qualified])) + \
            list(chain.from_iterable([Keywords.split_camel(c) for c in APICalls.from_call(callnode)])) + \
            list(chain.from_iterable([Keywords.split_camel(t) for t in Types.from_call(callnode)]))

        # convert to lower case, omit stop words and take the set
        return list(set([k.lower() for k in keywords if k.lower() not in Keywords.STOP_WORDS]))


# TODO: handle Javadoc with word2vec
class Javadoc(Evidence):

    def __init__(self, order, max_length, filter_sizes, num_filters):
        self.order = order
        self.max_sentence_length = max_length
        self.filter_sizes = filter_sizes
        self.num_filters = num_filters
        self.pad_char = '_PADDING_'

    def load_embedding(self, save_dir):
        embed_save_dir = os.path.join(save_dir, 'embed_javadoc')
        # vocabulary
        with open(os.path.join(embed_save_dir, 'config.json')) as f:
            js = json.load(f)
        self.chars = js['chars']
        # add padding character
        self.chars = [self.pad_char] + self.chars
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        # self.vocab_size = len(self.vocab)
        # max_sentence_length could also be pre-determined and hard-coded
        # self.max_sentence_length = js['javadoc_' + self.order + '_max_length']
        # embedding
        with tf.Session() as sess:
            embedding = tf.get_variable('embedding_' + self.order, [js['vocab_size'], js['embedding_size']],
                                        dtype=tf.float32, trainable=False)
            norm = tf.sqrt(tf.reduce_sum(tf.square(embedding), 1, keep_dims=True))
            normalized_embedding = embedding / norm
            saver = tf.train.Saver({'embedding': embedding})
            ckpt = tf.train.get_checkpoint_state(embed_save_dir)
            saver.restore(sess, ckpt.model_checkpoint_path)
            # numpy array
            self.final_embedding = normalized_embedding.eval()
            # add embedding for padding character
            self.final_embedding = np.append([[0] * js['embedding_size']], self.final_embedding, axis=0)

    # def read_data_point(self, program, infer=False):
    def read_data_point(self, program):
        # assume data is clean
        field_name = 'javadoc_' + self.order
        javadoc = program[field_name] if field_name in program else None
        if not javadoc:
            javadoc = UNK
        try:  # do not consider non-ASCII javadoc
            javadoc.encode('ascii')
        except UnicodeEncodeError:
            javadoc = UNK
        javadoc = javadoc.split()
        # if len(javadoc) > self.max_sentence_length:
        #     self.max_sentence_length = len(javadoc)
        # replace words not in the dictionary with unknown
        javadoc = [i if i in self.chars else UNK for i in javadoc]

        return javadoc

    def wrangle(self, data):
        # index words and pad and trim word vectors
        indices_list = []
        for s in data:
            padding_length = self.max_sentence_length - len(s)
            if padding_length > 0:
                s.extend([self.pad_char] * padding_length)
                words = s
            elif padding_length < 0:
                words = s[:padding_length]
            else:
                words = s
            indices = list(map(self.vocab.get, words))
            indices_list.append(indices)
        return np.array(indices_list, dtype=np.int32)

    def placeholder(self, config):
        return tf.placeholder(tf.int32, [config.batch_size, self.max_sentence_length])

    def exists(self, inputs):
        return tf.not_equal(tf.count_nonzero(inputs, axis=1), 0)

    def init_sigma(self, config):
        with tf.variable_scope('javadoc_' + self.order):
            self.sigma = tf.get_variable('sigma', [])

    def encode(self, inputs, config):
        with tf.variable_scope('javadoc_' + self.order):
            # step. make embedding a variable tensor
            # W = tf.get_variable(name="W", shape=embedding.shape, tf.constant_initializer(embedding), trainable=False)
            with tf.variable_scope('embedding'):
                W = tf.get_variable(
                    name='W',
                    shape=list(self.final_embedding.shape),
                    initializer=tf.constant_initializer(self.final_embedding),
                    trainable=False)
                embedded_chars = tf.nn.embedding_lookup(W, inputs)
                embedded_chars_expanded = tf.expand_dims(embedded_chars, -1)

            # convolution and pooling layers
            pooled_outputs = []
            embedding_size = self.final_embedding.shape[1]
            for i, filter_size in enumerate(self.filter_sizes):
                with tf.variable_scope('conv-maxpool-%s' % filter_size):
                    # convolution
                    filter_shape = [filter_size, embedding_size, 1, self.num_filters]
                    # hyper-parameters
                    W = tf.Variable(tf.truncated_normal(filter_shape, stddev=0.1), name='W')
                    b = tf.Variable(tf.constant(0.1, shape=[self.num_filters]), name='b')
                    conv = tf.nn.conv2d(
                        embedded_chars_expanded,
                        W,
                        strides=[1, 1, 1, 1],
                        padding='VALID',
                        name='conv')
                    # non-linearity
                    h = tf.nn.relu(tf.nn.bias_add(conv, b), name='relu')
                    # max pooling -> tensor([batch_size, 1, 1, num_filters])
                    pooled = tf.nn.max_pool(
                        h,
                        ksize=[1, self.max_sentence_length - filter_size + 1, 1, 1],
                        strides=[1, 1, 1, 1],
                        padding='VALID',
                        name='pool')
                    pooled_outputs.append(pooled)

            # combine all the pooled features
            num_filters_total = self.num_filters * len(self.filter_sizes)
            h_pool = tf.concat(pooled_outputs, 3)
            h_pool_flat = tf.reshape(h_pool, [-1, num_filters_total])

            # dropout
            with tf.variable_scope('dropout'):
                # hyper-parameter
                keep_rate = 0.5
                h_drop = tf.nn.dropout(h_pool_flat, keep_rate)

            # final fc layer
            with tf.variable_scope('output'):
                W = tf.get_variable(
                    'W',
                    shape=[num_filters_total, config.latent_size],
                    initializer=tf.contrib.layers.xavier_initializer())
                b = tf.Variable(tf.constant(0.1, shape=[config.latent_size]), name='b')
                latent_encoding = tf.nn.xw_plus_b(h_drop, W, b, name='encoding')
                return latent_encoding

    def evidence_loss(self, psi, encoding, config):
        sigma_sq = tf.square(self.sigma)
        loss = 0.5 * (config.latent_size * tf.log(2 * np.pi * sigma_sq + 1e-10)
                      + tf.square(encoding - psi) / sigma_sq)
        return loss

