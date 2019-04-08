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

from __future__ import print_function
import numpy as np
import tensorflow as tf

import argparse
import time
import os
import sys
import json
import textwrap

from bayou.models.low_level_evidences.data_reader import Reader
from bayou.models.low_level_evidences.model import Model
from bayou.models.low_level_evidences.utils import read_config, dump_config, get_var_list, static_plot, get_available_gpus


HELP = """\
Config options should be given as a JSON file (see config.json for example)
"""
#%%

def train(clargs):

    dataIsThere = False

    if clargs.continue_from is not None:
        config_file = os.path.join(clargs.continue_from, 'config.json')
    elif dataIsThere:
        config_file = os.path.join('data', 'config.json')
    else:
        config_file = clargs.config

    with open(config_file) as f:
        config = read_config(json.load(f), chars_vocab=(clargs.continue_from or dataIsThere))

    reader = Reader(clargs, config, dataIsThere=dataIsThere)

    # merged_summary = tf.summary.merge_all()

    # Placeholders for tf data

    nodes_placeholder = tf.placeholder(reader.nodes.dtype, reader.nodes.shape)
    parents_placeholder = tf.placeholder(reader.parents.dtype, reader.nodes.shape)
    edges_placeholder = tf.placeholder(reader.edges.dtype, reader.edges.shape)
    targets_placeholder = tf.placeholder(reader.targets.dtype, reader.targets.shape)
    evidence_placeholder = [tf.placeholder(input.dtype, input.shape) for input in reader.inputs]
    # reset batches

    feed_dict={fp: f for fp, f in zip(evidence_placeholder, reader.inputs)}

    feed_dict.update({nodes_placeholder: reader.nodes})
    feed_dict.update({parents_placeholder: reader.nodes})
    feed_dict.update({edges_placeholder: reader.edges})
    feed_dict.update({targets_placeholder: reader.targets})

    dataset = tf.data.Dataset.from_tensor_slices((nodes_placeholder, parents_placeholder, edges_placeholder, targets_placeholder, *evidence_placeholder))
    batched_dataset = dataset.batch(config.batch_size)
    iterator = batched_dataset.make_initializable_iterator()

    model = Model(config , iterator, bayou_mode=False)

    with tf.Session(config=tf.ConfigProto(log_device_placement=False, allow_soft_placement=True)) as sess:
        writer = tf.summary.FileWriter(clargs.save)
        writer.add_graph(sess.graph)
        tf.global_variables_initializer().run()

        tf.train.write_graph(sess.graph_def, clargs.save, 'model.pbtxt')
        tf.train.write_graph(sess.graph_def, clargs.save, 'model.pb', as_text=False)
        saver = tf.train.Saver(tf.global_variables(), max_to_keep=3)

        # restore model
        if clargs.continue_from is not None:
            bayou_vars = get_var_list()['bayou_vars']
            old_saver = tf.train.Saver(bayou_vars, max_to_keep=None)
            ckpt = tf.train.get_checkpoint_state(clargs.continue_from)
            old_saver.restore(sess, ckpt.model_checkpoint_path)

        # training
        #epocLoss , epocGenL , epocKlLoss = [], [], []
        for i in range(config.num_epochs):
            sess.run(iterator.initializer, feed_dict=feed_dict)
            start = time.time()
            avg_loss = 0. #, avg_gen_loss, avg_RE_loss , avg_FS_loss , avg_KL_loss = 0.,0.,0.,0.,0.
            for b in range(config.num_batches):
                # run the optimizer
                loss, _ = sess.run([model.loss, model.train_op])
                # allEvSigmas = sess.run(model.allEvSigmas)
                # s = sess.run(merged_summary, feed)
                # writer.add_summary(s,i)

                end = time.time()
                avg_loss += np.mean(loss)



                step = i * config.num_batches + b + 1
                if step % config.print_step == 0:
                    print('{}/{} (epoch {}) '
                          'loss: {:.3f}, \n\t'.format
                          (step, config.num_epochs * config.num_batches, i + 1 ,avg_loss/(b+1)))

            #epocLoss.append(avg_loss / config.num_batches), epocGenL.append(avg_gen_loss / config.num_batches), epocKlLoss.append(avg_KL_loss / config.num_batches)
            if (i+1) % config.checkpoint_step == 0:
                checkpoint_dir = os.path.join(clargs.save, 'model{}.ckpt'.format(i+1))
                saver.save(sess, checkpoint_dir)

                print('Model checkpointed: {}. Average for epoch , '
                      'loss: {:.3f}'.format
                      (checkpoint_dir, avg_loss / config.num_batches))
        #static_plot(epocLoss , epocGenL , epocKlLoss)


#%%
if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter,
                                     description=textwrap.dedent(HELP))
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--python_recursion_limit', type=int, default=10000,
                        help='set recursion limit for the Python interpreter')
    parser.add_argument('--save', type=str, default='save',
                        help='checkpoint model during training here')
    parser.add_argument('--config', type=str, default=None,
                        help='config file (see description above for help)')
    parser.add_argument('--continue_from', type=str, default=None,
                        help='ignore config options and continue training model checkpointed here')
    #clargs = parser.parse_args()
    clargs = parser.parse_args(
     # ['--continue_from', 'save',
     ['--config','config.json',
     '/home/ubuntu/CACM_data/DATA-extr-fr-CACM-wEvidence.json'])
    sys.setrecursionlimit(clargs.python_recursion_limit)
    if clargs.config and clargs.continue_from:
        parser.error('Do not provide --config if you are continuing from checkpointed model')
    if not clargs.config and not clargs.continue_from:
        parser.error('Provide at least one option: --config or --continue_from')
    train(clargs)
