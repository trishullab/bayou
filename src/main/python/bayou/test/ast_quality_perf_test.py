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
import time

from bayou.core.infer import BayesianPredictor
from bayou.server.ast_server import _generate_asts


def ast_quality_perf_test(clargs):
    with open(clargs.input_file[0]) as f:
        js = json.load(f)
    programs = js['programs']

    with tf.Session() as sess:
        print('Loading model...')
        predictor = BayesianPredictor(clargs.save, sess)

        n = len(programs)
        for i, program in enumerate(programs):
            start = time.time()
            if not clargs.evidence == 'all':
                if program[clargs.evidence] is []:
                    program['out_asts'] = []
                    latency = float('{:.2f}'.format(time.time() - start))
                    program['latency'] = latency
                    continue
                evidences = {clargs.evidence: program[clargs.evidence]}
                remaining = ['apicalls', 'types', 'context']
                remaining.remove(clargs.evidence)
                for ev in remaining:
                    evidences[ev] = []
            else:
                evidences = program

            result = json.loads(_generate_asts(json.dumps(evidences), predictor))

            program['out_asts'] = result['asts']
            latency = float('{:.2f}'.format(time.time() - start))
            program['latency'] = latency
            print('{}/{} done'.format(i, n))

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
    parser.add_argument('--evidence', type=str, default='all',
                        choices=['apicalls', 'types', 'context', 'all'],
                        help='use only this evidence for inference queries')
    parser.add_argument('--output_file', type=str, default=None,
                        help='output file to print predicted ASTs')
    clargs = parser.parse_args()
    print(clargs)
    ast_quality_perf_test(clargs)
