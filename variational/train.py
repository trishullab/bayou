from __future__ import print_function
import numpy as np
import tensorflow as tf

import argparse
import time
import os
import pickle

from utils import DataLoader
from model import Model

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('input_file', type=str, nargs=1,
                       help='input data file')
    parser.add_argument('--save_dir', type=str, default='save',
                       help='directory to store checkpointed models')
    parser.add_argument('--cell', type=str, default='rnn', choices=['rnn', 'lstm'],
                       help='type of RNN cell')
    parser.add_argument('--latent_size', type=int, default=8,
                       help='dimensions of latent space')
    parser.add_argument('--encoder_rnn_size', type=int, default=8,
                       help='size of encoder RNN hidden state')
    parser.add_argument('--decoder_rnn_size', type=int, default=128,
                       help='size of decoder RNN hidden state')
    parser.add_argument('--batch_size', type=int, default=50,
                       help='minibatch size')
    parser.add_argument('--max_seqs', type=int, default=16,
                       help='maximum number of sequences (including subsets) from a program')
    parser.add_argument('--max_seq_length', type=int, default=10,
                       help='maximum RNN sequence length')
    parser.add_argument('--max_ast_depth', type=int, default=20,
                       help='maximum depth of AST')
    parser.add_argument('--weight_loss', type=int, default=1000,
                       help='weight given to generation loss as opposed to latent loss')
    parser.add_argument('--num_epochs', type=int, default=50,
                       help='number of epochs')
    parser.add_argument('--learning_rate', type=float, default=0.002,
                       help='learning rate')
    parser.add_argument('--print_every', type=int, default=1,
                       help='print training output every n steps')
    parser.add_argument('--init_from', type=str, default=None,
                       help='continue training from previously checkpointed model saved here')
    args = parser.parse_args()
    train(args)


def train(args):
    data_loader = DataLoader(args.input_file[0], args)
    
    # check compatibility if training is continued from previously saved model
    if args.init_from is not None:
        ckpt = check_compat(args, data_loader)
        
    with open(os.path.join(args.save_dir, 'config.pkl'), 'wb') as f:
        pickle.dump(args, f)
    with open(os.path.join(args.save_dir, 'chars_vocab.pkl'), 'wb') as f:
        pickle.dump((data_loader.input_chars, data_loader.input_vocab,
                       data_loader. target_chars, data_loader.target_vocab), f)
    print(args)

    model = Model(args)

    with tf.Session() as sess:
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())

        # restore model
        if args.init_from is not None:
            saver.restore(sess, ckpt.model_checkpoint_path)

        # training
        for i in range(args.num_epochs):
            data_loader.reset_batches()
            for b in range(args.num_batches):
                start = time.time()

                # setup the feed dict
                x, l, n, e, y = data_loader.next_batch()
                init_state_mean = model.encoder.cell_mean_init.eval()
                init_state_stdv = model.encoder.cell_stdv_init.eval()
                feed = { model.targets: y,
                         model.encoder.cell_mean_init: init_state_mean,
                         model.encoder.cell_stdv_init: init_state_stdv }
                for j in range(args.max_seqs):
                    feed[model.encoder.seq[j].name] = x[j]
                    feed[model.encoder.seq_length[j].name] = l[j]
                for j in range(args.max_ast_depth):
                    feed[model.decoder.nodes[j].name] = n[j]
                    feed[model.decoder.edges[j].name] = e[j]

                # run the optimizer
                cost, latent, generation, mean, stdv, _ = sess.run([model.cost,
                                                                model.latent_loss,
                                                                model.generation_loss,
                                                                model.encoder.psi_mean,
                                                                model.encoder.psi_stdv,
                                                                model.train_op], feed)
                end = time.time()
                step = i * args.num_batches + b
                if step % args.print_every == 0:
                    print('{}/{} (epoch {}), latent: {:.3f}, generation: {:.3f}, cost: {:.3f}, '\
                            'mean: {:.3f}, stdv: {:.3f}, time: {:.3f}'.format(step,
                            args.num_epochs * args.num_batches, i, np.mean(latent), generation,
                            np.mean(cost), np.mean(mean), np.mean(stdv), end - start))
            checkpoint_path = os.path.join(args.save_dir, 'model.ckpt')
            saver.save(sess, checkpoint_path)
            print('model saved to {}'.format(checkpoint_path))


def check_compat(args, data_loader):
    # check if all necessary files exist 
    assert os.path.isdir(args.init_from),' %s must be a a path' % args.init_from
    assert os.path.isfile(os.path.join(args.init_from, 'config.pkl')), \
                'config.pkl file does not exist in path %s' % args.init_from
    assert os.path.isfile(os.path.join(args.init_from, 'chars_vocab.pkl')), \
                'chars_vocab.pkl.pkl file does not exist in path %s' % args.init_from
    ckpt = tf.train.get_checkpoint_state(args.init_from)
    assert ckpt,'No checkpoint found'
    assert ckpt.model_checkpoint_path,'No model path found in checkpoint'

    # open old config and check if models are compatible
    with open(os.path.join(args.init_from, 'config.pkl'), 'rb') as f:
        saved_model_args = pickle.load(f)
    need_be_same = ['cell', 'encoder_rnn_size', 'decoder_rnn_size', 'latent_size', 'max_seqs', 
                        'max_seq_length', 'max_ast_depth']
    for checkme in need_be_same:
        assert vars(saved_model_args)[checkme] == vars(args)[checkme], \
                    'Command line argument and saved model disagree on "%s" '%checkme
    
    # open saved vocab/dict and check if vocabs/dicts are compatible
    with open(os.path.join(args.init_from, 'chars_vocab.pkl'), 'rb') as f:
        input_chars, input_vocab, target_chars, target_vocab = pickle.load(f)
    assert input_chars == data_loader.input_chars and \
           input_vocab == data_loader.input_vocab and \
           target_chars == data_loader.target_chars and \
           target_vocab == data_loader.target_vocab, \
           'Data and saved model disagree on character vocabulary!'
    return ckpt

if __name__ == '__main__':
    main()
