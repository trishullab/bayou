import os
import re
import pickle
import itertools
import argparse

import numpy as np
import tensorflow as tf

CONFIG_GENERAL = ['cell', 'latent_size', 'batch_size', 'weight_loss', 'num_epochs', \
                 'learning_rate', 'print_step']
CONFIG_ENCODER = ['name', 'max_num', 'max_length', 'rnn_units', 'tile']
CONFIG_DECODER = ['rnn_units', 'max_ast_depth']
CONFIG_CHARS_VOCAB = ['chars', 'vocab', 'vocab_size']

C0 = 'CLASS0'
CHILD_EDGE = 'V'
SIBLING_EDGE = 'H'

def length(tensor):
    elems = tf.sign(tf.reduce_max(tensor, axis=2))
    return tf.reduce_sum(elems, axis=1)

def weighted_pick(weights):
    t = np.cumsum(weights)
    s = np.sum(weights)
    return int(np.searchsorted(t, np.random.rand(1)*s))

def get_keywords(seqs):

    def split_camel(s):
        s1 = re.sub('(.)([A-Z][a-z]+)', r'\1#\2', s) # LC followed by UC
        s1 = re.sub('([a-z0-9])([A-Z])', r'\1#\2', s1) # UC followed by LC
        return s1.split('#')

    def get_name(call):
        q = call.split('(')[0].split('.')
        cls, name = q[-2], q[-1]
        return cls + '#' + name

    calls = set([get_name(call) for sequence in seqs for call in sequence])
    keywords = list(itertools.chain.from_iterable([split_camel(call) for call in calls]))
    return list(set([kw.lower() for kw in keywords if not kw == '']))

def sub_sequences(seqs):
    sub_seqs = []
    for seq in seqs:
        cuts = [seq[:i] for i in range(1, len(seq)+1)]
        for s in cuts:
            if s not in sub_seqs:
                sub_seqs += [s]
    return sub_seqs

from variational.evidence import Evidence

# convert JSON to config
def read_config(js, chars_vocab):
    config = argparse.Namespace()

    for attr in CONFIG_GENERAL:
        config.__setattr__(attr, js[attr])
    
    config.evidence = Evidence.read_config(js['evidence'], chars_vocab)

    attrs = CONFIG_DECODER + (CONFIG_CHARS_VOCAB if chars_vocab else [])
    config.decoder = argparse.Namespace()
    for attr in attrs:
        config.decoder.__setattr__(attr, js['decoder'][attr])

    return config

# convert config to JSON
def dump_config(config):
    js = {}

    for attr in CONFIG_GENERAL:
        js[attr] = config.__getattribute__(attr)

    js['evidence'] = [ev.dump_config() for ev in config.evidence]

    attrs = CONFIG_DECODER + CONFIG_CHARS_VOCAB
    js['decoder'] = { attr: config.decoder.__getattribute__(attr) for attr in attrs }

    return js
