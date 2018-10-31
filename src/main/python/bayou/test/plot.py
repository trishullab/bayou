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
import ijson.backends.yajl2_cffi as ijson

import re
import numpy as np
import tensorflow as tf
from sklearn.manifold import TSNE

import matplotlib
matplotlib.use('TKAgg')
import matplotlib.pyplot as plt
import matplotlib.cm as cm

from scripts.ast_extractor import get_ast_paths
from bayou.models.low_level_evidences.infer import BayesianPredictor


def plot(clargs, max_nums=1000):
    with tf.Session() as sess:
        psis = []
        labels = []
        item_num = 0

        print('reading model')
        predictor = BayesianPredictor(clargs.save, sess, clargs.embedding_file)
        print('reading data')
        with open(clargs.input_file[0], 'rb') as f:
            for program in ijson.items(f, 'programs.item'):
                api_call = get_api(get_calls_from_ast(program['ast']['_nodes']))
                if api_call != 'N/A':
                    labels.append(api_call)
                    psis.append(predictor.psi_from_evidence(program)[0])
                    item_num += 1
                if item_num > max_nums:
                    break
        psis = np.array(psis)
        print('making graphs')
        model = TSNE(n_components=2, init='pca')
        psis_2d = model.fit_transform(psis)
        assert len(psis_2d) == len(labels)

        for psi_2d, label in zip(psis_2d, labels):
            print('{} : {}'.format(psi_2d, label))
        scatter(clargs, zip(psis_2d, labels))


# def get_api(calls):
#     apis = ['.'.join(call.split('.')[:2]) for call in calls]
#     counts = Counter(apis)
#     apis = sorted(counts.keys(), key=lambda a: counts[a], reverse=True)
#     return apis[0] if apis != [] else 'N/A'


def get_api(calls):
    calls = [call.replace('$NOT$', '') for call in calls]
    apis = [[re.findall(r"[\w']+", call)[:3]] for call in calls]
    apis = [call for _list in apis for calls in _list for call in calls]
    label = "N/A"
    guard = []
    for api in apis:
        if api in ['xml', 'sql', 'crypto', 'awt', 'swing', 'security', 'net', 'math']:
            label = api
            guard.append(label)

    if len(set(guard)) != 1:
        return 'N/A'
    else:
        return guard[0]


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

    plt.legend(plotpoints, labels, scatterpoints=1, loc='lower left', ncol=3, fontsize=8)
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
