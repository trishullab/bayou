import argparse
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
