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
from collections import Counter

import numpy as np
import tensorflow as tf
from sklearn.manifold import TSNE
import re
import random
from bayou.models.low_level_evidences.infer import BayesianPredictor
from scripts.ast_extractor import get_ast_paths

import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt

import matplotlib.cm as cm
import os

def plot(clargs):
    with tf.Session() as sess:
        predictor = BayesianPredictor(clargs.save, sess)
        with open(clargs.input_file[0]) as f:
            js = json.load(f)

        all_progs = js['programs']
        random.shuffle(all_progs)
        # all_progs = all_progs[:100]
        psis = np.array([predictor.psi_from_evidence(program) for program in all_progs])
        # random.shuffle(psis)
        ast_calls = [get_calls_from_ast(program['ast']['_nodes']) for program in all_progs]

        # for i, psi in enumerate(psis):
        #     print('Generate AST {}'.format(i))
        #     predictor.calls_in_last_ast = []
        #     try:
        #         ast = predictor.generate_asts_beam_search(psi, beam_width=25)
        #         # print(ast)
        #         ast_calls.append(get_calls_from_ast(ast[0]['ast']['_nodes']))
        #     except AssertionError:
        #         ast_calls.append([])
        psis = np.array([psi[0] for psi in psis])  # drop batch
        model = TSNE(n_components=2, init='random', learning_rate=200.0, perplexity=50)
        psis_2d = model.fit_transform(psis)
        labels = [get_api(calls) for calls in ast_calls]
        assert len(psis_2d) == len(labels)
        # for psi_2d, label in zip(psis_2d, labels):
        #     print('{} : {}'.format(psi_2d, label))
        scatter(clargs, zip(psis_2d, labels))


def get_api(calls):
    calls = [call.replace('$NOT$', '') for call in calls]
    apis = ['.'.join(re.findall(r"[\w']+", call)[:3]) for call in calls]
    counts = Counter(apis)
    counts['STOP'] = 0
    counts['DBranch'] = 0
    counts['DLoop'] = 0
    counts['DExcept'] = 0
    apis = sorted(counts.keys(), key=lambda a: counts[a], reverse=True)
    return apis[0] if apis != [] else 'N/A'



def get_calls_from_ast(ast):
    calls = []
    _, ast_paths = get_ast_paths(ast)
    for path in ast_paths:
        calls += [call[0] for call in path]
    return calls


def scatter(clargs, data):
    dic = {}
    for psi_2d, label in data:
        if label == 'N/A':
            continue
        if label not in dic:
            dic[label] = []
        dic[label].append(psi_2d)

    labels = list(dic.keys())
    labels.sort(key=lambda l: len(dic[l]), reverse=True)
    for label in labels[clargs.top:]:
        del dic[label]

    labels = dic.keys()
    colors = cm.rainbow(np.linspace(0, 1, len(dic)))
    plotpoints = []
    for label, color in zip(labels, colors):
        x = list(map(lambda s: s[0], dic[label]))
        y = list(map(lambda s: s[1], dic[label]))
        plotpoints.append(plt.scatter(x, y, color=color))

    plt.legend(plotpoints, labels, scatterpoints=1, loc='best', ncol=3, fontsize=12)
    plt.axhline(0, color='black')
    plt.axvline(0, color='black')
    plt.savefig(os.path.join(os.getcwd(), "tSNE.jpeg"), bbox_inches='tight')
    #plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--save', type=str, default='save',
                        help='directory to load model from')
    parser.add_argument('--top', type=int, default=10,
                        help='plot only the top-k labels')
    clargs = parser.parse_args()
    plot(clargs)
