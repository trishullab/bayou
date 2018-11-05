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


def predict(clargs, sample_times=10, beam_width=30, beam_reserve_cut=15):
    with tf.Session() as sess:
        out_programs = []
        predictor = BayesianPredictor(clargs.save, sess, clargs.embedding_file)
        print('reading data')
        program_count = 0
        with open(clargs.input_file[0], 'rb') as f:
            for program in ijson.items(f, 'programs.item'):
                program_count += 1
                print('current program 1-based index: %i' % program_count)
                out_asts = []
                for i in range(sample_times):
                    psi_batch = predictor.psi_from_evidence(program)
                    the_psi = psi_batch[0]
                    print(the_psi)
                    asts = predictor.generate_asts_beam_search(psi_batch, beam_width=beam_width)
                    for ast in asts:
                        ast['trial_index'] = i
                    out_asts.extend(asts)
                out_program = {'javadoc': program['javadoc']}
                out_asts_sorted = sorted(out_asts, reverse=True, key=lambda t: float(t['probability']))
                out_asts_cut = out_asts_sorted[:beam_reserve_cut]
                out_program['out_asts'] = out_asts_cut
                out_programs.append(out_program)
        with open(clargs.out, 'w') as f:
            json.dump({'programs': out_programs}, f, indent=2)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--save', type=str, default='save',
                        help='directory to load model from')
    parser.add_argument('--embedding_file', type=str, help='embedding file to use')
    parser.add_argument('--out', type=str, )
    clargs = parser.parse_args()
    predict(clargs)
