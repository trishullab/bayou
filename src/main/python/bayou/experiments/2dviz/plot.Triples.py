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

import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import os
import re
import numpy as np
import tensorflow as tf
from sklearn.manifold import TSNE

from scripts.ast_extractor import get_ast_paths
from bayou.models.low_level_evidences.predict import BayesianPredictor

def plot(clargs):
    sess = tf.InteractiveSession()
    predictor = BayesianPredictor(clargs.save, sess)
    with open(clargs.input_file[0], 'rb') as f:
        psis = []
        labels = []
        item_num = 0
        for program in ijson.items(f, 'programs.item'):
            #switch this on if you want to load pre-computed b1
            #switch this on if you are calculating based on evidences (which you can manipulate)
            #program = {'apicalls':program['apicalls'], 'types':program['types'], 'ast':program['ast']}

            api_call = get_api(get_calls_from_ast(program['ast']['_nodes']))
            if api_call != 'N/A':
            # if True:
                labels.append(api_call)
                psis.append(predictor.get_a1b1(program)[1][0])
                # psis.append(program['b1']) # b1 is basically a scaled (by a1) version of psis
                item_num += 1


        psis = np.array(psis)
        model = TSNE(n_components=2, init='random')
        psis_2d = model.fit_transform(psis)
        assert len(psis_2d) == len(labels)

        # for psi_2d, label in zip(psis_2d, labels):
        #     print('{} : {}'.format(psi_2d, label))

        scatter(clargs, zip(psis_2d, labels))


def get_api(calls):
    calls = [call.replace('$NOT$', '') for call in calls]
    apis = ['.'.join(re.findall(r"[\w']+", call)[:3]) for call in calls]


    apis = [call for call in apis]
    apis = [api if 'NOT' not in api else api[5:] for api in apis]
    counts = Counter(apis)
    counts['STOP'] = 0
    counts['DBranch'] = 0
    counts['DLoop'] = 0
    counts['DExcept'] = 0

    apis = sorted(counts.keys(), key=lambda a: counts[a], reverse=True)

    label = "N/A"
    retLabel = "N/A"
    guard = []
    for api in apis:
        # if 'io' == api:
        #     label = 'io'
        #     guard.append(label)
        if 'xml' in api:
            label = 'xml'
            guard.append(label)
            retLabel = api
        if 'sql' in api:
            label = 'sql'
            guard.append(label)
            retLabel = api
        if 'crypto' in api:
            label = 'crypto'
            guard.append(label)
            retLabel = api
        if 'awt' in api:
            label = 'awt'
            guard.append(label)
            retLabel = api
        if 'swing' in api:
            label = 'swing'
            guard.append(label)
            retLabel = api
        if 'security' in api:
            label = 'security'
            guard.append(label)
            retLabel = api
        if 'net' in api:
            label = 'net'
            guard.append(label)
            retLabel = api
        if 'math' in api:
            label = 'math'
            guard.append(label)
            retLabel = api


    if len(set(guard)) != 1:
        return 'N/A'
    else:
        return retLabel



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

    plt.legend(plotpoints, labels, scatterpoints=1, loc='lower left', ncol=3, fontsize=12)
    plt.axhline(0, color='black')
    plt.axvline(0, color='black')
    plt.savefig(os.path.join(os.getcwd(), "tSNE.jpeg"), bbox_inches='tight')
    # plt.show()


def get_calls_from_ast(ast):
    calls = []
    _, ast_paths = get_ast_paths(ast)
    for path in ast_paths:
        calls += [call[0] for call in path]
    return calls

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
