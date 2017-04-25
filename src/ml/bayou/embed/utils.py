import argparse

CONFIG_GENERAL = ['evidence', 'embedding_size', 'window_size', 'num_sampled', \
                    'batch_size', 'num_epochs', 'learning_rate', 'print_step']
CONFIG_CHARS_VOCAB = ['chars', 'vocab', 'vocab_size']

# convert JSON to config
def read_config(js, chars_vocab):
    config = argparse.Namespace()
    for attr in CONFIG_GENERAL:
        config.__setattr__(attr, js[attr])
    if chars_vocab:
        for attr in CONFIG_CHARS_VOCAB:
            config.__setattr__(attr, js[attr])
    
    return config

# convert config to JSON
def dump_config(config):
    js = {}
    for attr in CONFIG_GENERAL:
        js[attr] = config.__getattribute__(attr)
    for attr in CONFIG_CHARS_VOCAB:
        js[attr] = config.__getattribute__(attr)

    return js
