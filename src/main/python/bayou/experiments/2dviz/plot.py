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
from bayou.models.low_level_evidences.infer import BayesianPredictor
from bayou.models.low_level_evidences.data_reader import Reader
from bayou.models.low_level_evidences.utils import read_config

def plot(clargs):

    with open(os.path.join(clargs.save, 'config.json')) as f:
        config = read_config(json.load(f), chars_vocab=True)


    clargs.continue_from = None
    reader = Reader(clargs, config, infer=True)

	# Placeholders for tf data
    nodes_placeholder = tf.placeholder(reader.nodes.dtype, reader.nodes.shape)
    edges_placeholder = tf.placeholder(reader.edges.dtype, reader.edges.shape)
    targets_placeholder = tf.placeholder(reader.targets.dtype, reader.targets.shape)
    evidence_placeholder = [tf.placeholder(input.dtype, input.shape) for input in reader.inputs]

    # reset batches

    feed_dict={fp: f for fp, f in zip(evidence_placeholder, reader.inputs)}
    feed_dict.update({nodes_placeholder: reader.nodes})
    feed_dict.update({edges_placeholder: reader.edges})
    feed_dict.update({targets_placeholder: reader.targets})

    dataset = tf.data.Dataset.from_tensor_slices((nodes_placeholder, edges_placeholder, targets_placeholder, *evidence_placeholder))
    batched_dataset = dataset.batch(config.batch_size)
    iterator = batched_dataset.make_initializable_iterator()


    sess = tf.InteractiveSession()
    predictor = BayesianPredictor(clargs.save, sess, config, iterator)


    # Plot for indicidual evidences
    for ev in config.evidence:
        print(ev.name)
        with open(clargs.input_file[0], 'rb') as f:
            deriveAndScatter(f, predictor, [ev])

    # Plot with all Evidences
    with open(clargs.input_file[0], 'rb') as f:
        deriveAndScatter(f, predictor, [ev for ev in config.evidence])
    #
    # with open(clargs.input_file[0], 'rb') as f:
    #     useAttributeAndScatter(f, 'b2')


def useAttributeAndScatter(f, att, max_nums=10000):
    psis = []
    labels = []
    item_num = 0
    for program in ijson.items(f, 'programs.item'):
        api_call = get_api(get_calls_from_ast(program['ast']['_nodes']))
        if api_call != 'N/A':
            labels.append(api_call)
            if att not in program:
                return
            psis.append(program[att])
            item_num += 1

        if item_num > max_nums:
            break

    psis = np.array(psis)
    name = "RE" if att == "b2" else att
    fitTSEandplot(psis, labels, name)


def deriveAndScatter(f, predictor, evList, max_nums=10000):
    psis = []
    labels = []
    item_num = 0
    for program in ijson.items(f, 'programs.item'):
        shortProgram = {'ast':program['ast']}
        for ev in evList:
            if ev.name == "callsequences":
                ev.name = "sequences"
            if ev.name == "returntype":
                ev.name = "returnType"
            if ev.name == "formalparam":
                ev.name = "formalParam"
            shortProgram[ev.name] = program[ev.name]

        if len(evList) == 1 and len(program[evList[0].name]) == 0:
            continue

        api_call = get_api(get_calls_from_ast(shortProgram['ast']['_nodes']))
        if api_call != 'N/A':
            labels.append(api_call)
            psis.append(predictor.get_a1b1(shortProgram)[0])
            item_num += 1

        if item_num > max_nums:
            break

    name = "_".join([ev.name for ev in evList])
    fitTSEandplot(psis, labels, name)

def fitTSEandplot(psis, labels, name):
    model = TSNE(n_components=2, init='random')
    psis_2d = model.fit_transform(psis)
    assert len(psis_2d) == len(labels)
    scatter(clargs, zip(psis_2d, labels), name)

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



def scatter(clargs, data, name):
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
    plt.savefig(os.path.join(os.getcwd(), "plots/tSNE_" + name + ".jpeg"), bbox_inches='tight')
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
    parser.add_argument('--python_recursion_limit', type=int, default=10000,
                    help='set recursion limit for the Python interpreter')
    parser.add_argument('--save', type=str, default='save',
                        help='directory to load model from')
    parser.add_argument('--top', type=int, default=10,
                        help='plot only the top-k labels')
    clargs = parser.parse_args()
    plot(clargs)
