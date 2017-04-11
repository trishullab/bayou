from __future__ import print_function
import numpy as np
import tensorflow as tf

import argparse
import time
import os
import json
import textwrap

from bayou.data_reader import Reader
from bayou.model import Model
from bayou.evidence import Evidence
from bayou.utils import read_config, dump_config

HELP = """\
Config options should be given as a JSON file (see config.json for example):
{                                |
    "cell": "lstm",              | The type of RNN cell. Choices: lstm, rnn
    "latent_size": 10,           | Latent dimensionality
    "batch_size": 50,            | Minibatch size
    "num_epochs": 100,           | Number of training epochs
    "weight_loss": 1000,         | Weight given to generation loss as opposed to latent loss
    "learning_rate": 0.02,       | Learning rate
    "print_step": 1,             | Print training output every given steps
    "evidence": [                | Provide each evidence type in this list
        {                        |
            "name": "sequences", | Name of evidence ("sequences")
            "max_num": 20,       | Maximum number of sequences in each data point
            "max_length": 10,    | Maximum length of each sequence
            "rnn_units": 10,     | Size of the encoder hidden state
            "tile": 1            | Repeat the encoding n times (to boost its signal)
        },                       |  
        {                        | 
            "name": "keywords",  | Name of evidence ("keywords")                      
            "max_num": 20,       | Maximum number of keywords in each data point
            "max_length": 1,     | Keywords do not have a 2nd dimension (length)
            "rnn_units": 10,     | Size of the encoder hidden state
            "tile": 100          | Repeat the encoding n times (to boost its signal)
        },                       | 
        {                        | 
            "name": "javadoc",   | Name of evidence ("javadoc")                      
            "max_num": 1,        | Javadoc does not have first dimension (num)
            "max_length": 32,    | Maximum number of words in Javadoc
            "rnn_units": 10,     | Size of the encoder hidden state
            "tile": 100          | Repeat the encoding n times (to boost its signal)
        }                        | 
    ],                           |
    "decoder": {                 | Provide parameters for the decoder here
        "rnn_units": 100,        | Size of the decoder hidden state
        "max_ast_depth": 20      | Maximum depth of the AST (length of the longest path)
    }                            |
}                                |
"""

def train(clargs):
    config_file = clargs.config if clargs.continue_from is None \
                                else os.path.join(clargs.continue_from, 'config.json')
    with open(config_file) as f:
        config = read_config(json.load(f), clargs.continue_from)
    assert config.cell == 'lstm' or config.cell == 'rnn', 'Invalid cell in config'
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
                feed = { model.targets: y }
                for j, ev in enumerate(config.evidence):
                    for k in range(ev.max_num):
                        feed[model.encoder.inputs[j][k].name] = ev_data[j][k]
                for j in range(config.decoder.max_ast_depth):
                    feed[model.decoder.nodes[j].name] = n[j]
                    feed[model.decoder.edges[j].name] = e[j]
                for cell_init in model.encoder.init:
                    feed[cell_init] = cell_init.eval(session=sess)

                # run the optimizer
                cost, latent, generation, mean, stdv, _ = sess.run([model.cost,
                                                                model.latent_loss,
                                                                model.generation_loss,
                                                                model.encoder.psi_mean,
                                                                model.encoder.psi_stdv,
                                                                model.train_op], feed)
                end = time.time()
                step = i * config.num_batches + b
                if step % config.print_step == 0:
                    print('{}/{} (epoch {}), latent: {:.3f}, generation: {:.3f}, cost: {:.3f}, '\
                            'mean: {:.3f}, stdv: {:.3f}, time: {:.3f}'.format(step,
                            config.num_epochs * config.num_batches, i, np.mean(latent), generation,
                            np.mean(cost), np.mean(mean), np.mean(stdv), end - start))
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
