from __future__ import print_function
import tensorflow as tf

import argparse
import os
import json
import threading
import time
import traceback

from variational.predict_ast import VariationalPredictor
from variational.data_reader import sub_sequences
from variational.utils import get_keywords

def start_server(args):
    assert not os.path.exists(args.pipe), \
        'I think server is already running! If not, delete pipe: {}'.format(args.pipe)
    os.mkfifo(args.pipe)
    with tf.Session() as sess, open(args.log, 'a') as logfile:
        print('\n\n\nCreated pipe: {}. MAKE SURE USERS HAVE WRITE ACCESS TO IT\n\n'\
                .format(args.pipe))
        predictor = VariationalPredictor(args.save_dir, sess)
        log('Server started and listening to: {}'.format(args.pipe), logfile)
        req = 0
        try:
            while True:
                with open(args.pipe) as pipe:
                    content = pipe.read()
                    if content.rstrip('\n') == 'kill':
                        log('Received kill command', logfile)
                        break
                    threading.Thread(target=serve, args=(content, predictor)).start()
                    req += 1
                    log('Served request {}\n{}'.format(req, content), logfile)
        finally:
            os.remove(args.pipe)

def serve(content, predictor):
    try:
        outpipe = content.split('#')[0]
    except:
        return
    with open(outpipe, 'w') as out:
        try:
            js = json.loads(content.split('#')[1])
            assert 'keywords' in js or 'sequences' in js
            keywords, sequences = [], []
            if 'keywords' in js:
                keywords = js['keywords'].split()
            if 'sequences' in js:
                sequences = sub_sequences(js['sequences'], predictor.model.args)
            keywords = list(set(keywords + get_keywords(sequences)))
            keywords = [k for k in keywords if k in predictor.input_vocab_kws]

            asts = []
            for i in range(10):
                try:
                    psi = predictor.psi_from_evidence(sequences, keywords)
                    ast, p = predictor.generate_ast(psi)
                    ast['p_ast'] = p
                    asts.append(ast)
                except AssertionError:
                    continue
            json.dump({ 'asts': asts }, out, indent=2)
        except json.decoder.JSONDecodeError:
            out.write('ERROR: Malformed input. Please check if keywords (only characters) and sequences are valid JSON.')
        except AssertionError:
            out.write('ERROR: Provide at least one form of evidence: "keywords" or "sequences".')
        except Exception as e:
            out.write('ERROR: Unexpected error occurred during inference. Please try again.\n')
            traceback.print_exc(file=out)

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
