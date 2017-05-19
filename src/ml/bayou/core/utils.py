import argparse
import tensorflow as tf

CONFIG_GENERAL = ['cell', 'latent_size', 'batch_size', 'weight_loss', 'num_epochs',
                  'learning_rate', 'print_step']
CONFIG_ENCODER = ['name', 'units', 'tile']
CONFIG_DECODER = ['units', 'max_ast_depth']
CONFIG_DECODER_INFER = ['chars', 'vocab', 'vocab_size']

C0 = 'CLASS0'
UNK = '_UNK_'
CHILD_EDGE = 'V'
SIBLING_EDGE = 'H'


def length(tensor):
    elems = tf.sign(tf.reduce_max(tensor, axis=2))
    return tf.reduce_sum(elems, axis=1)

# Do not move this import to the top, it will introduce a cyclic dependency
import bayou.core.evidence


# convert JSON to config
def read_config(js, save_dir, infer=False):
    config = argparse.Namespace()

    for attr in CONFIG_GENERAL:
        config.__setattr__(attr, js[attr])
    
    config.evidence = bayou.core.evidence.Evidence.read_config(js['evidence'], save_dir)
    config.decoder = argparse.Namespace()
    for attr in CONFIG_DECODER:
        config.decoder.__setattr__(attr, js['decoder'][attr])
    if infer:
        for attr in CONFIG_DECODER_INFER:
            config.decoder.__setattr__(attr, js['decoder'][attr])

    return config


# convert config to JSON
def dump_config(config):
    js = {}

    for attr in CONFIG_GENERAL:
        js[attr] = config.__getattribute__(attr)

    js['evidence'] = [ev.dump_config() for ev in config.evidence]
    js['decoder'] = {attr: config.decoder.__getattribute__(attr) for attr in
                     CONFIG_DECODER + CONFIG_DECODER_INFER}

    return js
