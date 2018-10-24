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
import numpy as np
from sklearn.manifold import TSNE


def plot(data, useful_embedding):
    tsne_input = []
    labels = []
    curr_dim = 0
    for list in data:
        for word in list:
            tsne_input.append(useful_embedding[word])
            labels.append(str(curr_dim))
        curr_dim += 1
    tsne_input = np.array(tsne_input)
    model = TSNE(n_components=2, init='pca')
    tsne_result = model.fit_transform(tsne_input)
    scatter(zip(tsne_result, labels))


def scatter(data):
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
    for label in labels[10:]:
        del dic[label]

    labels = dic.keys()
    colors = cm.rainbow(np.linspace(0, 1, len(dic)))
    plotpoints = []
    for label, color in zip(labels, colors):
        x = list(map(lambda s: s[0], dic[label]))
        y = list(map(lambda s: s[1], dic[label]))
        plotpoints.append(plt.scatter(x, y, color=color))
    plt.show()


# returns latent dimensionality sized list of list of words
def read_dataset(clargs):
    word_lines = []
    # skip first 8 lines, information lines
    info_lines = 8
    file = open(clargs.data_file)
    lines = file.readlines()
    file.close()
    # 6 lines for each dimension
    dimensionality = (len(lines) - info_lines) / 6
    start_line = info_lines + 1 # 0-based
    for i in range(int(dimensionality)):
        word_lines.append(lines[start_line + i * 6].strip())
    list_of_words = []
    import ast
    for word_line in word_lines:
        list_of_words.append(ast.literal_eval(word_line))
    return list_of_words


# returns of a dict: token -> embedding (list of floats)
def get_useful_embedding(clargs, tokens):
    file = open(clargs.embedding_file)
    lines = file.readlines()
    file.close()
    embedding = {}
    for line in lines:
        splits = line.split(' ', 1)
        embedding[splits[0]] = splits[1]
    del lines
    useful_embedding = {}
    for token in tokens:
        useful_embedding[token] = [float(i) for i in embedding[token].split(' ')]
    del embedding
    return useful_embedding


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('data_file', type=str)
    parser.add_argument('embedding_file', type=str)
    clargs = parser.parse_args()
    data = read_dataset(clargs)
    import itertools
    tokens = list(itertools.chain.from_iterable(data))
    useful_embedding = get_useful_embedding(clargs, tokens)
    plot(data, useful_embedding)
