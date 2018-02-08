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
import bayou.models.low_level_evidences.evidence
import bayou.models.core.infer
import bayou.models.low_level_evidences.infer
from bayou.models.low_level_evidences.evidence import Keywords
from bayou.models.low_level_evidences.utils import gather_calls


# called when a POST request is sent to the server at the index path
def _handle_http_post_request_index(predictor):

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


# called when a GET request is sent to the server at the /asthealth path
def _handle_http_get_request_health():
    return Response("Ok")


# handle an asts generation request by generating asts
def _handle_generate_asts_request(request_dict, predictor):

    evidence_json_str = request_dict['evidence']  # get the evidence string from the request (also JSON)

    asts = _generate_asts(evidence_json_str, predictor)
    logging.debug(asts)
    return asts


def _generate_asts(evidence_json: str, predictor, okay_check=True):
    logging.debug("entering")

    js = json.loads(evidence_json)  # parse evidence as a JSON string

    # enhance keywords evidence from others
    keywords = list(chain.from_iterable([Keywords.split_camel(c) for c in js['apicalls']])) + \
        list(chain.from_iterable([Keywords.split_camel(t) for t in js['types']])) + \
        js['keywords']
    js['keywords'] = list(set([k.lower() for k in keywords if k.lower() not in Keywords.STOP_WORDS]))

    #
    # Generate ASTs from evidence.
    #
    asts = predictor.infer(js)

    #
    # If okay_check is set, retain only those asts that pass the _okay(...) filter. Otherwise retain all asts.
    #
    if okay_check:
        okay_asts = []
        for ast in asts:
            if _okay(js, ast, predictor):
                okay_asts.append(ast)
    else:
        okay_asts = asts

    logging.debug("exiting")
    return json.dumps({'evidences': js, 'asts': okay_asts}, indent=2)


# Include in here any conditions that dictate whether an AST should be returned or not
def _okay(js, ast, predictor):
    calls = [predictor.callmap[call['_call']] for call in gather_calls(ast['ast'])]
    apicalls = list(set(chain.from_iterable(
        [bayou.models.low_level_evidences.evidence.APICalls.from_call(call) for call in calls])))
    types = list(set(chain.from_iterable(
        [bayou.models.low_level_evidences.evidence.Types.from_call(call) for call in calls])))
    keywords = list(set(chain.from_iterable(
        [bayou.models.low_level_evidences.evidence.Keywords.from_call(call) for call in calls])))

    ev_okay = all([c in apicalls for c in js['apicalls']]) and all([t in types for t in js['types']]) \
        and all([k in keywords for k in js['keywords']])
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
        log_paths = [(d + "/ast_server.log") for d in args.logs_dir.split(os.pathsep)]

    # ensure the parent directory of each log path exists or create it
    for log_path in log_paths:
        if not os.path.exists(os.path.dirname(log_path)):
            os.makedirs(os.path.dirname(log_path))

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
            model = bayou.models.core.infer.BayesianPredictor
        elif model_type == 'lle':
            model = bayou.models.low_level_evidences.infer.BayesianPredictor
        else:
            raise ValueError('Invalid model type in config: ' + model_type)
        bp = model(args.save_dir, sess)  # create a predictor that can generates ASTs from evidence

        # route POST requests to / to _handle_http_post_request_index(...)
        http_server.add_url_rule("/", "index", lambda: _handle_http_post_request_index(bp), methods=['POST'])
        # route GET requests to /asthealth to _handle_http_get_request_health
        http_server.add_url_rule("/asthealth", "/asthealth", _handle_http_get_request_health, methods=['GET'])

        print("===================================")
        print("            Bayou Ready            ")
        print("===================================")
        http_server.run(host='0.0.0.0', port=8084)  # does not return
        _shutdown()  # we don't shut down flask directly, but if for some reason it ever stops go ahead and stop Bayou
