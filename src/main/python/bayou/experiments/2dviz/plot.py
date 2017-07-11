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

import json
import argparse
import numpy as np
import tensorflow as tf
from sklearn.manifold import TSNE

from bayou.core.infer import BayesianPredictor


def plot(clargs):
    with tf.Session() as sess:
        predictor = BayesianPredictor(clargs.save, sess)
        with open(clargs.input_file[0]) as f:
            js = json.load(f)
        psis = np.array([predictor.psi_from_evidence(program)[0] for program in js['programs']])
        model = TSNE(n_components=2, init='pca')
        psis_2d = model.fit_transform(psis)
        labels = [sorted(program['types'])[0] for program in js['programs']]
        assert len(psis_2d) == len(labels)

        for psi_2d, label in zip(psis_2d, labels):
            print('{} : {}'.format(psi_2d, label))
        scatter(clargs, zip(psis_2d, labels))


def scatter(clargs, data):
    import matplotlib.pyplot as plt
    import matplotlib.cm as cm
    dic = {}
    for psi_2d, label in data:
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
    clargs = parser.parse_args()
    plot(clargs)
