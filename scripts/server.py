from __future__ import print_function
import tensorflow as tf

import argparse
import os
import json
import threading
import time

from variational.predict_ast import VariationalPredictor
from variational.data_reader import sub_sequences
from variational.utils import get_keywords

def start_server(args):
    assert not os.path.exists(args.pipe), \
        'I think server is already running! If not, delete pipe: {}'.format(args.pipe)
    os.mkfifo(args.pipe)
    with tf.Session() as sess, open(args.log, 'w') as logfile:
        predictor = VariationalPredictor(args.save_dir, sess)
        log('Server started and listening to: {}'.format(args.pipe), logfile)
        req = 0
        try:
            while True:
                with open(args.pipe) as pipe:
                    content = pipe.read()
                    threading.Thread(target=serve, args=(content, predictor)).start()
                    req += 1
                    log('Served request {}\n{}'.format(req, content), logfile)
        finally:
            os.remove(args.pipe)

def serve(content, predictor):
    contents = content.split('\n')[:-1]
    outpipe = contents[0]
    keywords = contents[1].split()
    sequences = []
    if len(contents) > 2:
        sequences = sub_sequences(json.loads('\n'.join(contents[2:])), predictor.model.args)
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

def log(p, logfile):
    t = time.asctime()
    logfile.write('{} :: {}\n'.format(t, p))
    logfile.flush()
    os.fsync(logfile)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, required=True,
                       help='model directory to laod from')
    parser.add_argument('--pipe', type=str, required=True,
                       help='pipe file to listen to')
    parser.add_argument('--log', type=str, default='log.out',
                       help='log file')
    args = parser.parse_args()
    start_server(args)
