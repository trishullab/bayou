from __future__ import print_function
import numpy as np
import tensorflow as tf

import argparse
import time
import os
import json
import textwrap

from bayou.core.data_reader import Reader
from bayou.core.model import Model
from bayou.core.utils import read_config, dump_config

HELP = """\
Config options should be given as a JSON file (see config.json for example):
{                                         |
    "latent_size": 8,                     | Latent dimensionality
    "batch_size": 50,                     | Minibatch size
    "num_epochs": 500,                    | Number of training epochs
    "learning_rate": 0.02,                | Learning rate
    "print_step": 1,                      | Print training output every given steps
    "alpha": 0.001,                       | Hyper-param associated with KL-divergence loss
    "evidence": [                         | Provide each evidence type in this list
        {                                 |
            "name": "keywords",           | Name of evidence ("keywords")
            "units": 8,                   | Size of the encoder hidden state
            "tile": 1,                    | Repeat the encoding n times (to boost its signal)
        },                                |
        {                                 |
            "name": "types",              | Name of evidence ("types")
            "units": 8,                   | Size of the encoder hidden state
            "tile": 2,                    | Repeat the encoding n times (to boost its signal)
        },                                |
        {                                 |
            "name": "context",            | Name of evidence ("context")
            "units": 8,                   | Size of the encoder hidden state
            "tile": 2,                    | Repeat the encoding n times (to boost its signal)
        }                                 |
    ],                                    |
    "decoder": {                          | Provide parameters for the decoder here
        "units": 128,                     | Size of the decoder hidden state
        "max_ast_depth": 20               | Maximum depth of the AST (length of the longest path)
    }                                     |
}                                         |
"""


def train(clargs):
    config_file = clargs.config if clargs.continue_from is None \
                                else os.path.join(clargs.continue_from, 'config.json')
    with open(config_file) as f:
        config = read_config(json.load(f), save_dir=clargs.save)
    reader = Reader(clargs, config)
    
    jsconfig = dump_config(config)
    print(clargs)
    print(json.dumps(jsconfig, indent=2))
    with open(os.path.join(clargs.save, 'config.json'), 'w') as f:
        json.dump(jsconfig, fp=f, indent=2)

    model = Model(config)

    with tf.Session() as sess:
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())

        # restore model
        if clargs.continue_from is not None:
            ckpt = tf.train.get_checkpoint_state(clargs.continue_from)
            saver.restore(sess, ckpt.model_checkpoint_path)

        # training
        for i in range(config.num_epochs):
            reader.reset_batches()
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
                loss, latent, generation, mean, stdv, _ = sess.run([model.loss,
                                                                    model.latent_loss,
                                                                    model.gen_loss,
                                                                    model.encoder.psi_mean,
                                                                    model.encoder.psi_stdv,
                                                                    model.train_op], feed)
                end = time.time()
                step = i * config.num_batches + b
                if step % config.print_step == 0:
                    print('{}/{} (epoch {}), latent: {:.3f}, generation: {:.3f}, loss: {:.3f}, '
                          'mean: {:.3f}, stdv: {:.3f}, time: {:.3f}'.format
                          (step,
                           config.num_epochs * config.num_batches,
                           i,
                           np.mean(latent),
                           generation,
                           np.mean(loss),
                           np.mean(mean),
                           np.mean(stdv),
                           end - start))
            checkpoint_dir = os.path.join(clargs.save, 'model.ckpt')
            saver.save(sess, checkpoint_dir)
            print('Model checkpointed: {}'.format(checkpoint_dir))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter,
                                     description=textwrap.dedent(HELP))
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--save', type=str, default='save',
                        help='checkpoint model during training here')
    parser.add_argument('--config', type=str, default=None,
                        help='config file (see description above for help)')
    parser.add_argument('--continue_from', type=str, default=None,
                        help='ignore config options and continue training model checkpointed here')
    clargs = parser.parse_args()
    if clargs.config and clargs.continue_from:
        parser.error('Do not provide --config if you are continuing from checkpointed model')
    if not clargs.config and not clargs.continue_from:
        parser.error('Provide at least one option: --config or --continue_from')
    train(clargs)
