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
import time

import tensorflow as tf

import bayou.experiments.low_level_sketches.infer
import bayou.experiments.nonbayesian.infer
import bayou.models.core.infer
import bayou.models.low_level_evidences.infer

TIMEOUT = 20  # seconds per query


def main(clargs):
    with open(clargs.input_file[0]) as f:
        js = json.load(f)
    programs = js['programs']

    with tf.Session() as sess:
        if clargs.model == 'bayesian':
            p_type = bayou.models.core.infer.BayesianPredictor
        elif clargs.model == 'nonbayesian':
            p_type = bayou.experiments.nonbayesian.infer.NonBayesianPredictor
        elif clargs.model == 'low_level_evidences':
            p_type = bayou.models.low_level_evidences.infer.BayesianPredictor
        elif clargs.model == 'low_level_sketches':
            p_type = bayou.experiments.low_level_sketches.infer.BayesianPredictor
        else:
            raise TypeError('invalid type of model specified')
        print('Loading model...')
        predictor = p_type(clargs.save, sess)

        for i, program in enumerate(programs):
            start = time.time()
            if not clargs.evidence == 'all':
                if program[clargs.evidence] is []:
                    program['out_asts'] = []
                    print('Program {}, {} ASTs, {:.2f}s'.format(
                        i, len(program['out_asts']), time.time() - start))
                    continue
                evidences = {clargs.evidence: program[clargs.evidence]}
            else:
                evidences = program
            asts, counts = [], []
            for j in range(100):
                if time.time() - start > TIMEOUT:
                    break
                try:
                    ast = predictor.infer(evidences)
                except AssertionError:
                    continue
                try:
                    counts[asts.index(ast)] += 1
                except ValueError:
                    asts.append(ast)
                    counts.append(1)
            for ast, count in zip(asts, counts):
                ast['count'] = count
            asts.sort(key=lambda x: x['count'], reverse=True)
            program['out_asts'] = asts[:10]
            print('Program {}, {} ASTs, {:.2f}s'.format(
                i, len(program['out_asts']), time.time() - start))

        if clargs.output_file is None:
            print(json.dumps({'programs': programs}, indent=2))
        else:
            with open(clargs.output_file, 'w') as f:
                json.dump({'programs': programs}, f, indent=2)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--save', type=str, required=True,
                        help='directory to load model from')
    parser.add_argument('--model', type=str, required=True,
                        choices=['bayesian', 'nonbayesian', 'low_level_evidences', 'low_level_sketches'],
                        help='the type of model')
    parser.add_argument('--evidence', type=str, default='all',
                        choices=['apicalls', 'types', 'keywords', 'all'],
                        help='use only this evidence for inference queries')
    parser.add_argument('--output_file', type=str, default=None,
                        help='output file to print predicted ASTs')
    clargs = parser.parse_args()
    print(clargs)
    main(clargs)
