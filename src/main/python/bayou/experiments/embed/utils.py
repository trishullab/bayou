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

import argparse

CONFIG_GENERAL = ['embedding_size', 'window_size', 'num_sampled',
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
