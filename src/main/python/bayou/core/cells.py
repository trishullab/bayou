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
from tensorflow.python.ops.rnn_cell_impl import _RNNCell
import os


class PretrainedEmbeddingWrapper(_RNNCell):
    def __init__(self, cell, evidence):
        self.cell = cell
        self.evidence = evidence
        self.embedding = tf.get_variable('embedding', [evidence.vocab_size, evidence.units],
                                         dtype=tf.float32, trainable=False)
        norm = tf.sqrt(tf.reduce_sum(tf.square(self.embedding), 1, keep_dims=True))
        self.norm_embedding = self.embedding / norm

    @property
    def state_size(self):
        return self.cell.state_size

    @property
    def output_size(self):
        return self.cell.output_size

    def __call__(self, inputs, state, scope=None):
        with tf.variable_scope(scope or 'pretrained_embedding_wrapper'):
            with tf.device('/cpu:0'):
                emb_inputs = tf.nn.embedding_lookup(self.norm_embedding, tf.reshape(inputs, [-1]))
        return self.cell(emb_inputs, state)

    def load_from(self, sess, save_dir):
        saver = tf.train.Saver({'embedding': self.embedding})
        save = os.path.join(save_dir, 'embed_' + self.evidence.name)
        ckpt = tf.train.get_checkpoint_state(save)
        assert ckpt and ckpt.model_checkpoint_path, 'Could not find embeddings in {}'.format(save)
        saver.restore(sess, ckpt.model_checkpoint_path)
        print('Loaded embeddings for {} from {}'.format(self.evidence.name, save))
