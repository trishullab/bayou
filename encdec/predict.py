import tensorflow as tf

import argparse
import os
import ast
import pickle

from model import Model
from utils import weighted_pick

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, default='save',
                       help='model directory to laod from')
    parser.add_argument('--input_sequences', type=str, required=True,
                       help='set of input sequences')
    parser.add_argument('--prime', default='[("DBlock", "V")]',
                       help='prime output path')
    parser.add_argument('--topk', type=int, default=5,
                       help='print top-k values in distribution')

    args = parser.parse_args()
    predict(args)

def predict(args):
    # load the saved vocabularies
    with open(os.path.join(args.save_dir, 'config.pkl'), 'rb') as f:
        saved_args = pickle.load(f)
    with open(os.path.join(args.save_dir, 'chars_vocab.pkl'), 'rb') as f:
        _, input_vocab, target_chars, target_vocab = pickle.load(f)
    model = Model(saved_args, True)

    # parse the inputs
    seqs = ast.literal_eval(args.input_sequences)
    nodes, edges = zip(*ast.literal_eval(args.prime))

    with tf.Session() as sess:
        # restore the saved model
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(args.save_dir)
        if ckpt and ckpt.model_checkpoint_path:
            saver.restore(sess, ckpt.model_checkpoint_path)

            # predict with model
            dist = model.infer(sess, seqs, nodes, edges, input_vocab, target_vocab)
            dist_dict = [(target_chars[i], prob) for i, prob in enumerate(dist)]

            for node, prob in sorted(dist_dict, key=lambda x:x[1], reverse=True)[:args.topk]:
                print('{:.2f} : {}'.format(prob, node))

            prediction = target_chars[weighted_pick(dist)]
            print('prediction : {}'.format(prediction))

if __name__ == '__main__':
    main()
