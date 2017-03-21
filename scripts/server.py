from __future__ import print_function
import tensorflow as tf

import argparse
import os
import json
import threading

from variational.predict_ast import VariationalPredictor
from variational.data_reader import sub_sequences
from variational.utils import get_keywords

def start_server(args):
    with tf.Session() as sess:
        assert not os.path.exists(args.pipe), \
            'I think server is already running! If not, delete pipe: {}'.format(args.pipe)
        os.mkfifo(args.pipe)
        predictor = VariationalPredictor(args.save_dir, sess)
        print('Server started and listening to: {}'.format(args.pipe))
        try:
            while True:
                with open(args.pipe) as pipe:
                    content = pipe.read()
                    threading.Thread(target=serve, args=(content, predictor)).start()
        finally:
            os.remove(args.pipe)

def serve(content, predictor):
    contents = content.split('\n')[:-1]
    outpipe = contents[0]
    keywords = contents[1].split()
    sequences = []
    if len(contents) > 2:
        sequences = sub_sequences(json.loads('\n'.join(contents[2:])))
    keywords = list(set(keywords + get_keywords(sequences)))
    keywords = [k for k in keywords if k in predictor.input_vocab_kws]

    psi = predictor.psi_from_evidence(sequences, keywords)
    asts = []
    for i in range(10):
        try:
            ast, p = predictor.generate_ast(psi)
            ast['p_ast'] = p
            asts.append(ast)
        except AssertionError:
            continue
    with open(outpipe, 'w') as f:
        json.dump({ 'asts': asts }, f, indent=2)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, required=True,
                       help='model directory to laod from')
    parser.add_argument('--pipe', type=str, required=True,
                       help='pipe file to listen to')
    args = parser.parse_args()
    start_server(args)
