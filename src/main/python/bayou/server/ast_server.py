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

import argparse
import json
import logging.handlers
import os
from itertools import chain
from flask import request, Response, Flask

import tensorflow as tf
import bayou.core.evidence
import bayou.core.infer
import bayou.experiments.low_level_evidences.infer
from bayou.core.evidence import Keywords


# called when a POST request is sent to the server for AST generation
def _handle_http_request(predictor):

    request_json = request.data.decode("utf-8")  # read request string
    logging.debug("request_json:" + request_json)
    request_dict = json.loads(request_json)  # parse request as a JSON string

    request_type = request_dict['request type']

    if request_type == 'generate asts':
        asts = _handle_generate_asts_request(request_dict, predictor)
        return Response(asts, mimetype="application/json")
    elif request_type == 'shutdown':
        _shutdown()  # does not return

    return Response("")


# handle an asts generation request by generating asts
def _handle_generate_asts_request(request_dict, predictor):

    evidence_json_str = request_dict['evidence']  # get the evidence string from the request (also JSON)
    sample_count = request_dict.get('sample count', None)
    sample_count = int(sample_count) if sample_count is not None else None

    max_ast_count = request_dict.get('max ast count')
    max_ast_count = int(max_ast_count) if max_ast_count is not None else None

    if sample_count is not None:
        asts = _generate_asts(evidence_json_str, predictor, num_samples=sample_count, max_ast_count=max_ast_count)
    else:
        asts = _generate_asts(evidence_json_str, predictor, max_ast_count=max_ast_count)
    logging.debug(asts)
    return asts


def _generate_asts(evidence_json: str, predictor, num_samples: int=100, max_ast_count: int=10):
    logging.debug("entering")
    logging.debug("num_samples:" + str(num_samples))

    if num_samples < 1:
        raise ValueError("num_samples must be a natural number")

    if max_ast_count < 1:
        raise ValueError("max_asts_count must be a natural number")

    js = json.loads(evidence_json)  # parse evidence as a JSON string

    # enhance keywords evidence from others
    keywords = [Keywords.split_camel(c) for c in js['apicalls']] + \
               [Keywords.split_camel(t) for t in js['types']] + \
               [Keywords.split_camel(c) for c in js['context']]
    keywords = [kw.lower() for kw in list(chain.from_iterable(keywords))]
    js['keywords'] = list(set(js['keywords'] + keywords))

    #
    # Generate ASTs from evidence.
    #
    asts, counts = [], []
    for i in range(num_samples):
        try:
            ast = predictor.infer(js)
            ast['calls'] = list(set(predictor.calls_in_last_ast))

            if ast in asts:
                counts[asts.index(ast)] += 1
            else:
                asts.append(ast)
                counts.append(1)
        except AssertionError as e:
            logging.debug("AssertionError: " + str(e))
            continue

    for ast, count in zip(asts, counts):
        ast['count'] = count
    asts.sort(key=lambda x: x['count'], reverse=True)

    #
    # Retain up to max_ast_count asts that pass the okay(...) filter.
    #
    okay_asts = []
    i = 0
    while i < len(asts) and len(okay_asts) < max_ast_count:
        ast = asts[i]
        if _okay(js, ast):
            okay_asts.append(ast)
        i = i + 1

    logging.debug("exiting")
    return json.dumps({'evidences': js, 'asts': okay_asts}, indent=2)


# Include in here any conditions that dictate whether an AST should be returned or not
def _okay(js, ast):
    calls = ast['calls']
    apicalls = list(set(chain.from_iterable([bayou.core.evidence.APICalls.from_call(call) for call in calls])))
    types = list(set(chain.from_iterable([bayou.core.evidence.Types.from_call(call) for call in calls])))
    context = list(set(chain.from_iterable([bayou.core.evidence.Context.from_call(call) for call in calls])))

    ev_okay = all([c in apicalls for c in js['apicalls']]) and all([t in types for t in js['types']]) \
        and all([c in context for c in js['context']])
    return ev_okay


# terminates the Python process. Does not return.
def _shutdown():
    print("===================================")
    print("            Bayou Stopping         ")
    print("===================================")
    os._exit(0)


if __name__ == '__main__':

    # Parse command line args.
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, required=True, help='model directory to laod from')
    parser.add_argument('--logs_dir', type=str, required=False, help='the directories to store log information '
                                                                     'separated by the OS path separator')
    args = parser.parse_args()

    if args.logs_dir is None:
        dir_path = os.path.dirname(__file__)
        log_paths = [os.path.join(dir_path, "../../../logs/ast_server.log")]
    else:
        log_paths = [(dir + "/ast_server.log") for dir in args.logs_dir.split(os.pathsep)]

    # Create the logger for the application.
    logging.basicConfig(
        format='%(asctime)s,%(msecs)d %(levelname)-8s [%(threadName)s %(filename)s:%(lineno)d] %(message)s',
        datefmt='%d-%m-%Y:%H:%M:%S',
        level=logging.DEBUG,
        handlers=[logging.handlers.RotatingFileHandler(log_path, maxBytes=100000000, backupCount=9) for log_path in
                  log_paths])

    logging.debug("entering")  # can't move line up in program because logger not configured until this point

    # Set up HTTP server, but do no start it (yet).
    http_server = Flask(__name__)

    # Load model and start processing any sent requests.
    with tf.Session() as sess:
        logging.info("loading model")

        print("===================================")
        print("    Loading Model. Please Wait.    ")
        print("===================================")

        with open(os.path.join(args.save_dir, 'config.json')) as f:
            model_type = json.load(f)['model']
        if model_type == 'core':
            model = bayou.core.infer.BayesianPredictor
        elif model_type == 'lle':
            model = bayou.experiments.low_level_evidences.infer.BayesianPredictor
        else:
            raise ValueError('Invalid model type in config: ' + model_type)
        bp = model(args.save_dir, sess)  # create a predictor that can generates ASTs from evidence

        # route POST requests to / to handle_http_request
        http_server.add_url_rule("/", "index", lambda: _handle_http_request(bp), methods=['POST'])

        print("===================================")
        print("            Bayou Ready            ")
        print("===================================")
        http_server.run(host='127.0.0.1', port=8084)  # does not return
        _shutdown()  # we don't shut down flask directly, but if for some reason it ever stops go ahead and stop Bayou
