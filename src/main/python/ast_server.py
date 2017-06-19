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

import tensorflow as tf
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

                evidence_size_in_bytes = int.from_bytes(_read_bytes(4, client_socket), byteorder='big', signed=True) # how long is the evidence string?
                logging.debug(evidence_size_in_bytes)

                evidence = _read_bytes(evidence_size_in_bytes, client_socket).decode("utf-8") # read evidence string
                logging.debug(evidence)

                asts = _generate_asts(evidence, predictor) # use predictor to generate ASTs JSON from evidence
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

def _generate_asts(evidence_json, predictor):
    logging.debug("entering")
    js = json.loads(evidence_json) # parse evidence as a JSON string

    #
    # Generate ASTs from evidence.
    #
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
        except AssertionError as e:
            logging.debug("AssertionError: " + e)
            continue

    for ast, count in zip(asts, counts):
        ast['count'] = count
    asts.sort(key=lambda x: x['count'], reverse=True)

    logging.debug("exiting")
    return json.dumps({'evidences': js, 'asts': asts[:10]}, indent=2) # return all ASTs as a JSON string


if __name__ == '__main__':

    # Parse command line args.
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, required=True,help='model directory to laod from')
    parser.add_argument('--logs_dir', type=str, required=False, help='the directory to store log information')
    args = parser.parse_args()

    if args.logs_dir is None:
        dirpath = os.path.dirname(__file__);
        logpath = os.path.join(dirpath, "../logs/ast_server.log")
    else:
        logpath = args.logs_dir + "/ast_server.log"

    # Create the logger for the application.
    logging.basicConfig(format='%(asctime)s,%(msecs)d %(levelname)-8s [%(filename)s:%(lineno)d] %(message)s',
                        datefmt='%d-%m-%Y:%H:%M:%S',
                        level=logging.DEBUG,
                        handlers=[logging.handlers.RotatingFileHandler(logpath, maxBytes=100000000, backupCount=9)])

    # Start processing requests.
    _start_server(args.save_dir)
