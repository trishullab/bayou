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
# from bayou.models.low_level_evidences.model import Model
from bayou.models.low_level_evidences.MultiGPUModel import MultiGPUModel
from bayou.models.low_level_evidences.utils import read_config, dump_config, get_available_gpus

HELP = """\
Config options should be given as a JSON file (see config.json for example):
{                                         |
    "model": "lle"                        | The implementation id of this model (do not change)
    "latent_size": 32,                    | Latent dimensionality
    "batch_size": 50,                     | Minibatch size
    "num_epochs": 100,                    | Number of training epochs
    "learning_rate": 0.02,                | Learning rate
    "print_step": 1,                      | Print training output every given steps
    "alpha": 1e-05,                       | Hyper-param associated with KL-divergence loss
    "beta": 1e-05,                        | Hyper-param associated with evidence loss
    "evidence": [                         | Provide each evidence type in this list
        {                                 |
            "name": "apicalls",           | Name of evidence ("apicalls")
            "units": 64,                  | Size of the encoder hidden state
            "num_layers": 3               | Number of densely connected layers
            "tile": 1                     | Repeat the encoding n times (to boost its signal)
        },                                |
        {                                 |
            "name": "types",              | Name of evidence ("types")
            "units": 32,                  | Size of the encoder hidden state
            "num_layers": 3               | Number of densely connected layers
            "tile": 1                     | Repeat the encoding n times (to boost its signal)
        },                                |
        {                                 |
            "name": "keywords",           | Name of evidence ("keywords")
            "units": 64,                  | Size of the encoder hidden state
            "num_layers": 3               | Number of densely connected layers
            "tile": 1                     | Repeat the encoding n times (to boost its signal)
        }                                 |
    ],                                    |
    "decoder": {                          | Provide parameters for the decoder here
        "units": 256,                     | Size of the decoder hidden state
        "num_layers": 3,                  | Number of layers in the decoder
        "max_ast_depth": 32               | Maximum depth of the AST (length of the longest path)
    }                                     |
}                                         |
"""


def train(clargs):
    config_file = clargs.config if clargs.continue_from is None \
                                else os.path.join(clargs.continue_from, 'config.json')
    with open(config_file) as f:
        config = read_config(json.load(f), chars_vocab=clargs.continue_from)
    # for attention branch
    config.embedding_file = clargs.embedding_file
    reader = Reader(clargs, config)
    
    jsconfig = dump_config(config)
    # print(clargs)
    # print(json.dumps(jsconfig, indent=2))
    with open(os.path.join(clargs.save, 'config.json'), 'w') as f:
        json.dump(jsconfig, fp=f, indent=2)

    # read data through Dataset API
    # https://zhuanlan.zhihu.com/p/30751039

    # Placeholders for tf data
    nodes_placeholder = tf.placeholder(reader.nodes.dtype, reader.nodes.shape)
    edges_placeholder = tf.placeholder(reader.edges.dtype, reader.edges.shape)
    targets_placeholder = tf.placeholder(reader.targets.dtype, reader.targets.shape)
    evidence_placeholder = [tf.placeholder(input.dtype, input.shape) for input in reader.inputs]
    # reset batches

    feed_dict={fp: f for fp, f in zip(evidence_placeholder, reader.inputs)}
    feed_dict.update({nodes_placeholder: reader.nodes})
    feed_dict.update({edges_placeholder: reader.edges})
    feed_dict.update({targets_placeholder: reader.targets})

    dataset = tf.data.Dataset.from_tensor_slices((nodes_placeholder, edges_placeholder, targets_placeholder, *evidence_placeholder))
    batched_dataset = dataset.batch(config.batch_size)
    iterator = batched_dataset.make_initializable_iterator()

    # test code for tf.data.Dataset
    # with tf.Session() as sess:
    #     sess.run(iterator.initializer, feed_dict=feed_dict)
    #     for i in range(10):
    #         test_value = sess.run(iterator.get_next())
    #         print('placeholder')

    model = MultiGPUModel(config, iterator)

    with tf.Session(config=tf.ConfigProto(log_device_placement=False, allow_soft_placement=True)) as sess:
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())

        if clargs.continue_from is not None:
            ckpt = tf.train.get_checkpoint_state(clargs.continue_from)
            saver.restore(sess, ckpt.model_checkpoint_path)

        devices = get_available_gpus()

        epocLoss, epocGenLoss, epocLatentLoss, epocEvLoss = [], [], [], []
        for i in range(config.num_epochs):
            sess.run(iterator.initializer, feed_dict=feed_dict)
            start = time.time()
            avg_loss, avg_gen_loss, avg_latent_loss, avg_ev_loss = 0.,0.,0.,0.
            for b in range(config.num_batches // len(devices)): # number of rounds per epoch
                # run the optimizer
                loss, gen_loss, latent_loss, ev_loss, _ = sess.run([model.avg_loss, model.avg_gen_loss, model.avg_latent_loss, model.avg_evidence_loss, model.apply_gradient_op])

                # s = sess.run(merged_summary, feed)
                # writer.add_summary(s,i)

                end = time.time()
                avg_loss += np.mean(loss)
                avg_gen_loss += np.mean(gen_loss)
                avg_latent_loss += np.mean(latent_loss)
                avg_ev_loss += np.mean(ev_loss)

                step = i * config.num_batches + b * len(devices)
                if step % config.print_step == 0:
                    print('{}/{} (epoch {}) '
                          'loss: {:.3f}, gen_loss: {:.3f}, latent_loss: {:.3f}, ev_loss: {:.3f},\n\t'.format
                          (step, config.num_epochs * config.num_batches, i + 1 ,
                           (avg_loss)/(b+1), (avg_gen_loss)/(b+1), (avg_latent_loss)/(b+1), (avg_ev_loss)/(b+1)
                           ))

            epocLoss.append(avg_loss / config.num_batches), epocGenLoss.append(avg_gen_loss / config.num_batches), epocLatentLoss.append(avg_latent_loss / config.num_batches), epocEvLoss.append(avg_ev_loss / config.num_batches)
            if (i+1) % config.checkpoint_step == 0:
                checkpoint_dir = os.path.join(clargs.save, 'model{}.ckpt'.format(i+1))
                saver.save(sess, checkpoint_dir)
                print('Model checkpointed: {}. Average for epoch , '
                      'loss: {:.3f}'.format
                      (checkpoint_dir, avg_loss / (config.num_batches // len(devices))))
    sys.exit()

    # ==================================================

    model = Model(config)

    with tf.Session() as sess:
        # # tensorboard code
        # tf.summary.tensor_summary('model_probs', model.probs)
        # merged = tf.summary.merge_all()
        # log_dir = 'bayou_tb_log'
        # if tf.gfile.Exists(log_dir):
        #     tf.gfile.DeleteRecursively(log_dir)
        # writer = tf.summary.FileWriter(log_dir, sess.graph)

        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        tf.train.write_graph(sess.graph_def, clargs.save, 'model.pbtxt')
        tf.train.write_graph(sess.graph_def, clargs.save, 'model.pb', as_text=False)

        # restore model
        if clargs.continue_from is not None:
            ckpt = tf.train.get_checkpoint_state(clargs.continue_from)
            saver.restore(sess, ckpt.model_checkpoint_path)

        # training
        for i in range(config.num_epochs):
            reader.reset_batches()
            avg_loss = avg_evidence = avg_latent = avg_generation = 0
            for b in range(config.num_batches):
                start = time.time()

                # setup the feed dict
                ev_data, n, e, y = reader.next_batch()
                feed = {model.targets: y}
                for j, ev in enumerate(config.evidence):
                    feed[model.encoder.inputs[j].name] = ev_data[j]
                for j in range(config.decoder.max_ast_depth):
                    feed[model.decoder.nodes[j].name] = n[j]
                    feed[model.decoder.edges[j].name] = e[j]

                # run the optimizer
                loss, evidence, latent, generation, mean, covariance, _ \
                    = sess.run([model.loss,
                                model.evidence_loss,
                                model.latent_loss,
                                model.gen_loss,
                                model.encoder.psi_mean,
                                model.encoder.psi_covariance,
                                model.train_op], feed)
                end = time.time()
                # writer.add_summary(summary, i)
                avg_loss += np.mean(loss)
                avg_evidence += np.mean(evidence)
                avg_latent += np.mean(latent)
                avg_generation += generation
                step = i * config.num_batches + b
                if step % config.print_step == 0:
                    print('{}/{} (epoch {}), evidence: {:.3f}, latent: {:.3f}, generation: {:.3f}, '
                          'loss: {:.3f}, mean: {:.3f}, covariance: {:.3f}, time: {:.3f}'.format
                          (step, config.num_epochs * config.num_batches, i,
                           np.mean(evidence),
                           np.mean(latent),
                           generation,
                           np.mean(loss),
                           np.mean(mean),
                           np.mean(covariance),
                           end - start))
            checkpoint_dir = os.path.join(clargs.save, 'model{}.ckpt'.format(i))
            saver.save(sess, checkpoint_dir)
            print('Model checkpointed: {}. Average for epoch evidence: {:.3f}, latent: {:.3f}, '
                  'generation: {:.3f}, loss: {:.3f}'.format
                  (checkpoint_dir,
                   avg_evidence / config.num_batches,
                   avg_latent / config.num_batches,
                   avg_generation / config.num_batches,
                   avg_loss / config.num_batches))


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
    # for attention branch
    parser.add_argument('--embedding_file', type=str, default=None, help='word embedding file for keywords')
    clargs = parser.parse_args()
    sys.setrecursionlimit(clargs.python_recursion_limit)
    if clargs.config and clargs.continue_from:
        parser.error('Do not provide --config if you are continuing from checkpointed model')
    if not clargs.config and not clargs.continue_from:
        parser.error('Provide at least one option: --config or --continue_from')
    train(clargs)

