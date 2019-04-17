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
import numpy as np
import tensorflow as tf

import argparse
import os
import sys
import simplejson as json
import textwrap

import time
import random

from bayou.models.low_level_evidences.infer import BayesianPredictor
from bayou.models.low_level_evidences.utils import read_config
from bayou.models.low_level_evidences.data_reader import Reader
from bayou.models.low_level_evidences.node import plot_path

evidence1 = {
    "apicalls": [
        "remove"
      ],
      "types": [
        "HashMap",
        "ArrayList"
      ]
    }


evidence2 = {
     "apicalls": [
         "readLine"
       ],
       "types": [
         "BufferedReader",
         "FileReader"
       ]
     }

evidence3 = {
    "apicalls": [
        "readLine",
        "split",
        "add"
      ],
      "types": [
        "BufferedReader",
        "FileReader",
        "ArrayList"
      ]
    }

evidence4 = {
    "apicalls": [
        "write"
      ],
      "types": [
        "BufferedWriter",
        "FileWriter"
      ]
    }

evidence5 = {
   "apicalls": [
       "next",
       "remove"
     ],
     "types": [
       "Iterator"
     ]
   }


evidence_dict = {'trash':evidence1, 'reader':evidence2, 'splitter':evidence3, 'writer':evidence4, 'iterator':evidence5}


def test(clargs):
    clargs.continue_from = True #None

    with open(os.path.join(clargs.save, 'config.json')) as f:
        config = read_config(json.load(f), chars_vocab=True)

    config.decoder.max_ast_depth = 1
    iWantRandom = False

    if (iWantRandom):
        config.batch_size = 1
    else:
        config.batch_size = 10

    predictor = BayesianPredictor(clargs.save, config) # goes to infer.BayesianPredictor
    # testing
    # sess.run(iterator.initializer, feed_dict=feed_dict)

    # allEvSigmas = predictor.get_ev_sigma()
    # print(allEvSigmas)

    evidence = evidence_dict[clargs.evidence]

    # print (evidence)

    ## breadth_first_search
    if (iWantRandom):
        path_head = predictor.random_search(evidence)
        path = path_head.breadth_first_search()

        randI = random.randint(0,1000)
        dot = plot_path(randI,path)
        # print(randI)
        # print(path)
    else:
        ## BEAM SEARCH
        candies = predictor.beam_search(evidence, topK=config.batch_size)
        for i, candy in enumerate(candies):
             path = candy.head.breadth_first_search()
             prob = candy.log_probabilty
        #
             dot = plot_path(i,path, prob)
        #     print(path)
        #     # print()
        #jsons = predictor.get_jsons_from_beam_search(evidence, topK=config.batch_size)

        #with open('output_jsons.json', 'w') as f:
        #    json.dump({'evidences': evidence, 'asts': jsons}, fp=f, indent=2)


    return



#%%
if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter)

    parser.add_argument('--python_recursion_limit', type=int, default=10000,
                        help='set recursion limit for the Python interpreter')
    parser.add_argument('--save', type=str, required=True,
                        help='checkpoint model during training here')
    parser.add_argument('--evidence', type=str, default='evidence1',
                        help='use only this evidence for inference queries')
    parser.add_argument('--output_file', type=str, default=None,
                        help='output file to print probabilities')

    #clargs = parser.parse_args()
    clargs = parser.parse_args()

    sys.setrecursionlimit(clargs.python_recursion_limit)
    test(clargs)
