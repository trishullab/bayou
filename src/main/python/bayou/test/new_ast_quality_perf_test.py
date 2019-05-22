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

from __future__ import print_function

import argparse
import os
import sys
import json
import math

from itertools import chain
from random import sample
from bayou.models.low_level_evidences.infer import BayesianPredictor
from bayou.models.low_level_evidences.utils import read_config
from bayou.models.low_level_evidences.evidence import Keywords


def sample_prog(prog, percentage):
    sample_pool = []
    for label_name in 'apicalls','types','keywords':
        for label_instance in prog[label_name]:
            sample_pool.append((label_name, label_instance))
    pool_size = len(sample_pool)
    sample_size = math.ceil(pool_size * percentage)
    # reset labels
    for label_name in 'apicalls', 'types', 'keywords':
        prog[label_name] = []
    samples = sample(sample_pool, sample_size)
    for label_name, label_instance in samples:
        prog[label_name].append(label_instance)


def infer_asts(clargs):
    with open(os.path.join(clargs.save, 'config.json')) as f:
        config = read_config(json.load(f), chars_vocab=True)

    config.decoder.max_ast_depth = 1
    config.batch_size = 20

    predictor = BayesianPredictor(clargs.save, config)

    print('reading %s' % clargs.input_file[0])
    with open(clargs.input_file[0]) as f:
        programs = json.load(f)['programs']

    for i, prog in enumerate(programs):
        print('processing %ith program' % i)
        sample_prog(prog, clargs.percentage)
        keywords = list(chain.from_iterable([Keywords.split_camel(c) for c in prog['apicalls']])) + \
            list(chain.from_iterable([Keywords.split_camel(t) for t in prog['types']])) + prog['keywords']
        prog['keywords'] = list(set([k.lower() for k in keywords if k.lower() not in Keywords.STOP_WORDS]))
        js = predictor.get_jsons_from_beam_search(prog, topK=config.batch_size)
        programs[i]['out_asts'] = js

    with open(clargs.output_file, 'w') as f:
        json.dump({'programs': programs}, f, indent=2)
    print('done!!!')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--python_recursion_limit', type=int, default=10000,
                        help='set recursion limit for the Python interpreter')
    parser.add_argument('--save', type=str, required=True,
                        help='checkpoint model during training here')
    parser.add_argument('--output_file', type=str, default=None,
                        help='output file to print probabilities')
    parser.add_argument('--percentage', type=float, default=1.0,
                        help='proportion of all evidence instances to be used, e.g. ')
    clargs = parser.parse_args()
    print(clargs)
    sys.setrecursionlimit(clargs.python_recursion_limit)
    infer_asts(clargs)
