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
import socket
from itertools import chain

import tensorflow as tf
import bayou.core.evidence
from bayou.core.infer import BayesianPredictor

TIMEOUT = 10 # seconds


def _start_server(save_dir):
    logging.debug("entering")

    with tf.Session() as sess:
        logging.info("loading model")

        print("===================================")
        print("    Loading Model. Please Wait.    ")
        print("===================================")

        predictor = BayesianPredictor(save_dir, sess) # create a predictor that can generates ASTs from evidence

        #
        # Create a socket listening to localhost:8084
        #
        server_socket = socket.socket()
        server_socket.bind(('localhost', 8084))
        server_socket.listen(20)
        logging.info("server listening")

        print("===================================")
        print("            Bayou Ready            ")
        print("===================================")

        #
        # 1.) Wait for an incoming client connection.
        # 2.) Read the first 4 bytes sent and interpret as a signed 32-bit big-endian integer.
        # 3.) Read the next k bytes specified by the integer and interpret as UTF-8 "evidence" string.
        # 4.) Generate a collection of ASTs in JSON form via serve(...) using the evidence.
        # 5.) Encode the JSON as a UTF-8 string.
        # 6.) Transmit the number of bytes used for the encoded string of step 5) as a signed 32-bit big-endian integer to the client.
        # 7.) Transmit the bytes of the string from step 5)
        #
        while True:
            try:
                client_socket, addr = server_socket.accept()  # await client connection
                logging.info("connection accepted")

                request_size_in_bytes = int.from_bytes(_read_bytes(4, client_socket), byteorder='big', signed=True)
                logging.debug(request_size_in_bytes)

                request_json = _read_bytes(request_size_in_bytes, client_socket).decode("utf-8")  # read request string
                logging.debug(request_json)

                request_dict = json.loads(request_json)  # parse request as a JSON string
                evidence_json_str = request_dict['evidence'] # get the evidence string from the request (also JSON)
                sample_count = request_dict.get('sample count', None)
                max_ast_count = request_dict.get('max ast count')

                if sample_count is not None:
                    asts = _generate_asts(evidence_json_str, predictor, num_samples=sample_count, max_ast_count=max_ast_count)
                else:
                    asts = _generate_asts(evidence_json_str, predictor, max_ast_count=max_ast_count)
                logging.debug(asts)

                _send_string_response(asts, client_socket)
                client_socket.close()
            except Exception as e:
                try:
                    logging.exception(str(e))
                    _send_string_response(json.dumps({ 'evidences': [], 'asts': [] }, indent=2), client_socket)
                    client_socket.close()
                except Exception as e1:
                    pass

def _send_string_response(string, client_socket):
    string_bytes = bytearray()
    string_bytes.extend(string.encode("utf-8")) # get the UTF-8 encoded bytes of the ASTs JSON

    client_socket.sendall(len(string_bytes).to_bytes(4, byteorder='big', signed=True))  # send result length
    client_socket.sendall(string_bytes) # send result

def _read_bytes(byte_count, connection):
    """ read the next byte_count bytes from connection and return the results as a byte array """
    num_left_to_read = byte_count
    buffer = bytearray(byte_count)
    view = memoryview(buffer)
    while num_left_to_read > 0:
        num_bytes_read = connection.recv_into(view, num_left_to_read)
        view = view[num_bytes_read:] # on next loop start writing bytes at the next empty position in buffer, not at start of buffer
        num_left_to_read-= num_bytes_read

    return buffer


# Include in here any conditions that dictate whether an AST should be returned or not
def okay(js, ast):
    calls = ast['calls']
    apicalls = list(set(chain.from_iterable([bayou.core.evidence.APICalls.from_call(call) for call in calls])))
    types = list(set(chain.from_iterable([bayou.core.evidence.Types.from_call(call) for call in calls])))
    context = list(set(chain.from_iterable([bayou.core.evidence.Context.from_call(call) for call in calls])))

    ev_okay = all([c in apicalls for c in js['apicalls']]) and all([t in types for t in js['types']]) \
        and all([c in context for c in js['context']])
    return ev_okay


def _generate_asts(evidence_json, predictor, num_samples=100, max_ast_count=10):
    logging.debug("entering")
    logging.debug("num_samples: " + str(num_samples))

    if num_samples < 1:
        raise ValueError("num_samples must be a natural number")

    if max_ast_count < 1:
        raise ValueError("max_asts_count must be a natural number")

    js = json.loads(evidence_json) # parse evidence as a JSON string

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
        if okay(js, ast):
            okay_asts.append(ast)
        i = i + 1


    logging.debug("exiting")
    return json.dumps({'evidences': js, 'asts': okay_asts}, indent=2)


if __name__ == '__main__':

    # Parse command line args.
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, required=True,help='model directory to laod from')
    parser.add_argument('--logs_dir', type=str, required=False, help='the directories to store log information seperated by the OS path seperator')
    args = parser.parse_args()

    if args.logs_dir is None:
        dirpath = os.path.dirname(__file__);
        logpaths = [os.path.join(dirpath, "../logs/ast_server.log")]
    else:
        logpaths = [(dir + "/ast_server.log") for dir in args.logs_dir.split(os.pathsep)]

    # Create the logger for the application.
    for logpath in logpaths:
        logging.basicConfig(format='%(asctime)s,%(msecs)d %(levelname)-8s [%(threadName)s %(filename)s:%(lineno)d] %(message)s',
                            datefmt='%d-%m-%Y:%H:%M:%S',
                            level=logging.DEBUG,
                            handlers=[logging.handlers.RotatingFileHandler(logpath, maxBytes=100000000, backupCount=9) for logpath in logpaths])

    # Start processing requests.
    _start_server(args.save_dir)
