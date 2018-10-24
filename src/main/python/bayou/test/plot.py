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

from scripts.ast_extractor import get_ast_paths
from bayou.models.low_level_evidences.infer import BayesianPredictor


def plot(clargs):
    with tf.Session() as sess:
        print('reading model')
        predictor = BayesianPredictor(clargs.save, sess, clargs.embedding_file)
        print('reading data')
        with open(clargs.input_file[0]) as f:
            js = json.load(f)
        print('generating psis')
        psis = np.array([predictor.psi_from_evidence(program) for program in js['programs']])
        ast_calls = []
        for i, psi in enumerate(psis):
            print('Generate AST {}'.format(i))
            predictor.calls_in_last_ast = []
            try:
                asts = predictor.generate_asts_beam_search(psi, beam_width=10)
                # most likely ast resulted from beam search
                the_ast = asts[0]
                ast_calls.append(get_calls_from_ast(the_ast['ast']['_nodes']))
            except AssertionError:
                ast_calls.append([])
        psis = np.array([psi[0] for psi in psis])  # drop batch
        print('making graphs')
        model = TSNE(n_components=2, init='pca')
        psis_2d = model.fit_transform(psis)
        labels = [get_api(calls) for calls in ast_calls]
        assert len(psis_2d) == len(labels)

        for psi_2d, label in zip(psis_2d, labels):
            print('{} : {}'.format(psi_2d, label))
        scatter(clargs, zip(psis_2d, labels))


def get_api(calls):
    apis = ['.'.join(call.split('.')[:2]) for call in calls]
    counts = Counter(apis)
    apis = sorted(counts.keys(), key=lambda a: counts[a], reverse=True)
    return apis[0] if apis != [] else 'N/A'


def get_calls_from_ast(ast):
    calls = []
    _, ast_paths = get_ast_paths(ast)
    for path in ast_paths:
        calls += [call[0] for call in path]
    return calls


def scatter(clargs, data):
    import matplotlib as mpl
    mpl.use('TKAgg')
    import matplotlib.pyplot as plt
    import matplotlib.cm as cm
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

    plt.legend(plotpoints, labels, scatterpoints=1, loc='lower left', ncol=3, fontsize=12)
    plt.axhline(0, color='black')
    plt.axvline(0, color='black')
    plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--save', type=str, default='save',
                        help='directory to load model from')
    parser.add_argument('--top', type=int, default=10,
                        help='plot only the top-k labels')
    parser.add_argument('--embedding_file', type=str, help='embedding file to use')
    clargs = parser.parse_args()
    plot(clargs)
