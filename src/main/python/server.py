from __future__ import print_function
import tensorflow as tf

import argparse
import os
import json
import threading
import time
import traceback

from bayou.core.infer import BayesianPredictor


def start_server(clargs):
    assert not os.path.exists(clargs.pipe), \
        'I think server is already running! If not, delete pipe: {}'.format(clargs.pipe)
    os.mkfifo(clargs.pipe)
    with tf.Session() as sess, open(clargs.log, 'a') as logfile:
        print('\n\n\nCreated pipe: {}. MAKE SURE USERS HAVE WRITE ACCESS TO IT\n\n'
              .format(clargs.pipe))
        predictor = BayesianPredictor(clargs.save, sess)
        log('Server started and listening to: {}'.format(clargs.pipe), logfile)
        req = 0
        try:
            while True:
                with open(clargs.pipe) as pipe:
                    content = pipe.read()
                    if content.rstrip('\n') == 'kill':
                        log('Received kill command', logfile)
                        break
                    threading.Thread(target=serve, args=(content, predictor)).start()
                    req += 1
                    log('Served request {}\n{}'.format(req, content), logfile)
        finally:
            os.remove(clargs.pipe)


def serve(content, predictor):
    try:
        outpipe = content.split('#')[0]
    except:
        return
    with open(outpipe, 'w') as out:
        try:
            js = json.loads(content.split('#')[1])
            asts, counts = [], []
            for i in range(100):
                try:
                    psi = predictor.psi_from_evidence(js)
                    ast = predictor.generate_ast(psi)
                    if ast in asts:
                        counts[asts.index(ast)] += 1
                    else:
                        asts.append(ast)
                        counts.append(1)
                except AssertionError:
                    continue
            for ast, count in zip(asts, counts):
                ast['count'] = count
            asts.sort(key=lambda x: x['count'], reverse=True)
            json.dump({'evidences': js, 'asts': asts[:10]}, out, indent=2)
        except json.decoder.JSONDecodeError:
            out.write('ERROR: Malformed input.')
        except AssertionError:
                out.write('ERROR: Given evidence does not follow saved model config')
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
    parser.add_argument('--save', type=str, required=True,
                        help='directory to laod model from')
    parser.add_argument('--pipe', type=str, required=True,
                        help='pipe file to listen to')
    parser.add_argument('--log', type=str, default='log.out',
                        help='log file')
    clargs = parser.parse_args()
    start_server(clargs)
