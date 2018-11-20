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
import ijson.backends.yajl2_cffi as ijson

import numpy as np
import tensorflow as tf

from bayou.models.low_level_evidences.infer import BayesianPredictor
from bayou.models.low_level_evidences.utils import CHILD_EDGE, SIBLING_EDGE

model = None

# def predict(clargs, sample_times=10, beam_width=30, beam_reserve_cut=15):
#     with tf.Session() as sess:
#         out_programs = []
#         predictor = BayesianPredictor(clargs.save, sess, clargs.embedding_file)
#         print('reading data')
#         program_count = 0
#         with open(clargs.input_file[0], 'rb') as f:
#             for program in ijson.items(f, 'programs.item'):
#                 program_count += 1
#                 print('current program 1-based index: %i' % program_count)
#                 out_asts = []
#                 for i in range(sample_times):
#                     psi_batch = predictor.psi_from_evidence(program)
#                     the_psi = psi_batch[0]
#                     print(the_psi)
#                     asts = predictor.generate_asts_beam_search(psi_batch, beam_width=beam_width)
#                     for ast in asts:
#                         ast['trial_index'] = i
#                     out_asts.extend(asts)
#                 out_program = {'javadoc': program['javadoc']}
#                 out_asts_sorted = sorted(out_asts, r everse=True, key=lambda t: float(t['probability']))
#                 out_asts_cut = out_asts_sorted[:beam_reserve_cut]
#                 out_program['out_asts'] = out_asts_cut
#                 out_programs.append(out_program)
#         with open(clargs.out, 'w') as f:
#             json.dump({'programs': out_programs}, f, indent=2)


def compute_probs(clargs):
    with tf.Session() as sess:
        predictor = BayesianPredictor(clargs.save, sess, clargs.embedding_file)

        out_programs = []
        count = 0
        with open(clargs.input_file[0], 'rb') as f:
            for program in ijson.items(f, 'programs.item'):
                count += 1
                if count % 1 == 0:
                    print('current program 1-based index: %i' % count)
                prob = ast_javadoc_cond_prob(predictor, program, sess)
                program['ast']['cond_prob'] = prob
                out_programs.append(program)
        with open(clargs.out, 'w') as f:
            json.dump({'programs': out_programs}, f, indent=2)


def ast_javadoc_cond_prob(predictor, program, sess):
    # javadoc = program['javadoc']
    ast = program['ast']
    ast_paths = get_ast_paths(ast['_nodes'])
    try:
        validate_sketch_paths(program, ast_paths)
    except (TooLongPathError, InvalidSketchError) as e:
        return -1
    psi_batch = predictor.psi_from_evidence(program)
    # the_psi = psi_batch[0]
    cache = dict()
    probability = 1.
    for path in ast_paths:
        # probability for ('DSubTree', 'V') is 1
        path = [('DSubTree', 'V')] + path
        path_nodes, path_edges = zip(*path)
        for i in range(len(path) - 1):
            nodes, edges = zip(*path[:i+1])
            dist = predictor.model.infer_ast(sess, psi_batch, nodes, edges, cache=cache)
            curr_node = path_nodes[i+1]
            curr_node_prob = dist[predictor.model.config.decoder.vocab[curr_node]]
            # import pdb; pdb.set_trace()
            probability *= curr_node_prob
    return probability


def get_ast_paths(js, idx=0):
    cons_calls = []
    i = idx
    while i < len(js):
        if js[i]['node'] == 'DAPICall':
            cons_calls.append((js[i]['_call'], SIBLING_EDGE))
        else:
            break
        i += 1
    if i == len(js):
        cons_calls.append(('STOP', SIBLING_EDGE))
        return [cons_calls]
    node_type = js[i]['node']

    if node_type == 'DBranch':
        pC = get_ast_paths(js[i]['_cond'])  # will have at most 1 "path"
        assert len(pC) <= 1
        p1 = get_ast_paths(js[i]['_then'])
        p2 = get_ast_paths(js[i]['_else'])
        p = [p1[0] + path for path in p2] + p1[1:]
        pv = [cons_calls + [('DBranch', CHILD_EDGE)] + pC[0] + path for path in p]
        p = get_ast_paths(js, i+1)
        ph = [cons_calls + [('DBranch', SIBLING_EDGE)] + path for path in p]
        return ph + pv

    if node_type == 'DExcept':
        p1 = get_ast_paths(js[i]['_try'])
        p2 = get_ast_paths(js[i]['_catch'])
        p = [p1[0] + path for path in p2] + p1[1:]
        pv = [cons_calls + [('DExcept', CHILD_EDGE)] + path for path in p]
        p = get_ast_paths(js, i+1)
        ph = [cons_calls + [('DExcept', SIBLING_EDGE)] + path for path in p]
        return ph + pv

    if node_type == 'DLoop':
        pC = get_ast_paths(js[i]['_cond'])  # will have at most 1 "path"
        assert len(pC) <= 1
        p = get_ast_paths(js[i]['_body'])
        pv = [cons_calls + [('DLoop', CHILD_EDGE)] + pC[0] + path for path in p]
        p = get_ast_paths(js, i+1)
        ph = [cons_calls + [('DLoop', SIBLING_EDGE)] + path for path in p]
        return ph + pv

def _check_DAPICall_repeats(nodelist):
    """
    Checks if an API call node repeats in succession twice in a list of nodes

    :param nodelist: list of nodes to check
    :return: None
    :raise: InvalidSketchError if some API call node repeats, ValueError if a node is of invalid type
    """
    for i in range(1, len(nodelist)):
        node = nodelist[i]
        node_type = node['node']
        if node_type == 'DAPICall':
            if nodelist[i] == nodelist[i-1]:
                raise InvalidSketchError
        elif node_type == 'DBranch':
            _check_DAPICall_repeats(node['_cond'])
            _check_DAPICall_repeats(node['_then'])
            _check_DAPICall_repeats(node['_else'])
        elif node_type == 'DExcept':
            _check_DAPICall_repeats(node['_try'])
            _check_DAPICall_repeats(node['_catch'])
        elif node_type == 'DLoop':
            _check_DAPICall_repeats(node['_cond'])
            _check_DAPICall_repeats(node['_body'])
        else:
            raise ValueError('Invalid node type: ' + node)

def validate_sketch_paths(program, ast_paths):
    """
    Checks if a sketch along with its paths is good training data:
    1. No API call should be repeated successively
    2. No path in the sketch should be of length more than max_ast_depth hyper-parameter
    3. No branch, loop or except should occur more than once along a single path

    :param program: the sketch
    :param ast_paths: paths in the sketch
    :return: None
    :raise: TooLongPathError or InvalidSketchError if sketch or its paths is invalid
    """
    _check_DAPICall_repeats(program['ast']['_nodes'])
    for path in ast_paths:
        if len(path) >= 32:
            raise TooLongPathError
        nodes = [node for (node, edge) in path]
        if nodes.count('DBranch') > 1 or nodes.count('DLoop') > 1 or nodes.count('DExcept') > 1:
            raise TooLongPathError
        calls = [call for (call, edge) in path if call not in ['DSubTree', 'DBranch', 'DLoop', 'DExcept', 'STOP']]
        for call in calls:
            if nodes.count(call) > 1:
                raise TooLongPathError


class TooLongPathError(Exception):
    pass


class InvalidSketchError(Exception):
    pass


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--save', type=str, default='save',
                        help='directory to load model from')
    parser.add_argument('--embedding_file', type=str, help='embedding file to use')
    parser.add_argument('--out', type=str, )
    clargs = parser.parse_args()
    compute_probs(clargs)