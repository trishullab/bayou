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

import tensorflow as tf

import argparse
import json
import socket
import logging
import logging.handlers

from variational.predict_ast import VariationalPredictor
from variational.data_reader import sub_sequences
from variational.utils import get_keywords


def _start_server(save_dir, logger):
    with tf.Session() as sess:
        predictor = VariationalPredictor(save_dir, sess) # create a predictor that can generates ASTs from evidence

        #
        # Create a socket listening to localhost:8084
        #
        server_socket = socket.socket()
        server_socket.bind(('localhost', 8084))
        server_socket.listen(20)
        logger.info("server listening")

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
                logger.info("connection accepted")

                evidence_size_in_bytes = int.from_bytes(_read_bytes(4, client_socket), byteorder='big', signed=True) # how long is the evidence string?
                logger.debug(evidence_size_in_bytes)

                evidence = _read_bytes(evidence_size_in_bytes, client_socket).decode("utf-8") # read evidence string
                logger.debug(evidence)

                asts = _generate_asts(evidence, predictor, logger) # use predictor to generate ASTs JSON from evidence
                logger.debug(asts)

                _send_string_response(asts, client_socket)
                client_socket.close()
            except Exception as e:
                try:
                    logger.exception(str(e))
                    _send_string_response(json.dumps({'asts': []}, indent=2), client_socket)
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

def _generate_asts(evidence_json, predictor, logger):
    js = json.loads(evidence_json) # parse evidence as a JSON string

    #
    # Extract keywords and sequences from given evidence.
    #
    if not('keywords' in js or 'sequences' in js):
        raise ValueError("expected keywords or sequences in evidence.  Found: " + evidence)

    keywords, sequences = [], []
    if 'keywords' in js:
        keywords = js['keywords'].split()
    if 'sequences' in js and len(js['sequences']) > 0:
        list_of_call_objects = js['sequences']
        first_obj = list_of_call_objects[0]
        call_list = first_obj['calls']
        logger.debug(str(call_list))
        sequences = sub_sequences([call_list], predictor.model.args)
    keywords = list(set(keywords + get_keywords(sequences)))
    keywords = [k for k in keywords if k in predictor.input_vocab_kws]

    #
    # Generate ASTs from sequences and keywords in evidence.
    #
    asts = []
    for i in range(10):
        try:
            psi = predictor.psi_from_evidence(sequences, keywords)
            ast, p = predictor.generate_ast(psi)
            ast['p_ast'] = p
            asts.append(ast)
        except AssertionError:
            continue

    return json.dumps({'asts': asts}, indent=2) # return all ASTs as a JSON string


if __name__ == '__main__':

    # Create the logger for the application.
    logger = logging.getLogger('logger')
    logger.setLevel(logging.DEBUG)
    logger.addHandler(logging.handlers.RotatingFileHandler('python_tf_server.log', maxBytes=100000000, backupCount=9))

    # Parse command line args.
    parser = argparse.ArgumentParser()
    parser.add_argument('--save_dir', type=str, required=True,help='model directory to laod from')
    args = parser.parse_args()

    # Start processing requests.
    _start_server(args.save_dir, logger)
