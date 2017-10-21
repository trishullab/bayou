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
import os
import sys
import json
import pickle
import argparse

from bayou.lda.model import LDA


def train(clargs):
    print('Reading data file...')
    data = get_data(clargs.input_file[0], clargs.evidence)

    ok = 'r'
    while ok == 'r':
        model = LDA(args=clargs)
        model.train(data)
        top_words = model.top_words(clargs.top)
        for i, words in enumerate(top_words):
            print('\nTop words in Topic#{:d}'.format(i))
            for w in words:
                print('{:.2f} {:s}'.format(words[w], w))
        if clargs.confirm:
            print('\nOK with the model (y(es)/n(o)/r(edo))? ', end='')
            ok = sys.stdin.readline().rstrip('\n')
        else:
            ok = 'y'

    if ok == 'y':
        print('Saving model to {:s}'.format(os.path.join(clargs.save, 'model.pkl')))
        with open(os.path.join(clargs.save, 'model.pkl'), 'wb') as fmodel:
            pickle.dump((model.model, model.vectorizer), fmodel)


def get_data(input_file, evidence):
    with open(input_file) as f:
        js = json.loads(f.read())
    data = []
    nprograms = len(js['programs'])

    for i, program in enumerate(js['programs']):
        bow = set(program[evidence])
        data.append(bow)
        print('Gathering data for LDA: {:5d}/{:d} programs'.format(i+1, nprograms), end='\r')
    print()
    return data


if __name__ == '__main__':
    argparser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    argparser.add_argument('input_file', type=str, nargs=1,
                           help='input JSON file')
    argparser.add_argument('--ntopics', type=int, required=True,
                           help='run LDA with n topics')
    argparser.add_argument('--evidence', choices=['apicalls', 'types', 'context', 'keywords'], required=True,
                           help='the type of evidence for which LDA is run')
    argparser.add_argument('--save', type=str, default='save',
                           help='directory to store LDA model')
    argparser.add_argument('--alpha', type=float, default=None,
                           help='initial doc-topic prior value')
    argparser.add_argument('--beta', type=float, default=None,
                           help='initial topic-word prior value')
    argparser.add_argument('--top', type=int, default=5,
                           help='top-k words to print from each topic')
    argparser.add_argument('--confirm', action='store_true',
                           help='confirm topics before saving')
    clargs = argparser.parse_args()
    train(clargs)
