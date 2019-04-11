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
import json
import ijson.backends.yajl2_cffi as ijson
import numpy as np
import random
import os
import pickle
from collections import Counter
from copy import deepcopy

from bayou.models.low_level_evidences.utils import gather_calls, dump_config
from bayou.models.low_level_evidences.node import Node, get_ast, CHILD_EDGE, SIBLING_EDGE



class Reader():
    def __init__(self, clargs, config, infer=False, dataIsThere=False):
        self.infer = infer
        self.config = config

        if clargs.continue_from is not None or dataIsThere:
            with open('data/inputs.txt', 'rb') as f:
                self.inputs = pickle.load(f)
            with open('data/nodes_edges_targets.txt', 'rb') as f:
                [self.nodes , self.edges, self.targets] = pickle.load(f)

            config.num_batches = int(len(self.nodes) / config.batch_size)

        else:
            random.seed(12)
            # read the raw evidences and targets
            print('Reading data file...')
            raw_evidences, raw_data_points  = self.read_data(clargs.input_file[0], infer, save=clargs.save)
            raw_evidences = [[raw_evidence[i] for raw_evidence in raw_evidences] for i, ev in
                             enumerate(config.evidence)]


            config.num_batches = int(len(raw_data_points) / config.batch_size)
            assert config.num_batches > 0, 'Not enough data'

            sz = config.num_batches * config.batch_size
            for i in range(len(raw_evidences)):
                raw_evidences[i] = raw_evidences[i][:sz]
            raw_data_points = raw_data_points[:sz]

        # setup input and target chars/vocab
            if clargs.continue_from is None:
                config.decoder.vocab, config.decoder.vocab_size = self.decoder_api_dict.get_call_dict()

            # wrangle the evidences and targets into numpy arrays
            self.inputs = [ev.wrangle(data) for ev, data in zip(config.evidence, raw_evidences)]
            self.nodes = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)
            self.edges = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.bool)
            self.targets = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)

            for i, path in enumerate(raw_data_points):
                len_path = min(len(path) , config.decoder.max_ast_depth)
                mod_path = path[:len_path]

                self.nodes[i, :len_path]   =  [ p[0] for p in mod_path ]
                self.edges[i, :len_path]   =  [ p[1] for p in mod_path ]
                self.targets[i, :len_path] =  [ p[2] for p in mod_path ]


            with open('data/inputs.txt', 'wb') as f:
                pickle.dump(self.inputs, f)
            with open('data/nodes_edges_targets.txt', 'wb') as f:
                pickle.dump([self.nodes , self.edges, self.targets] , f)

            jsconfig = dump_config(config)
            with open(os.path.join(clargs.save, 'config.json'), 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)
            with open('data/config.json', 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)



    def read_data(self, filename, infer, save=None):


        data_points = []
        done = 0
        self.decoder_api_dict = decoderDict(infer, self.config.decoder)

        f = open(filename , 'rb')
        for program in ijson.items(f, 'programs.item'):

            evidences = [ev.read_data_point(program, infer) for ev in self.config.evidence]
            ast_node_graph = get_ast(program['ast']['_nodes'])

            path = ast_node_graph.dfs()
            parsed_data_array = []
            for curr_node_val, parent_node_id, edge_type in path:
                curr_node_id = self.decoder_api_dict.get_or_add_node_val_from_callMap(curr_node_val)
                # now parent id is already evaluated since this is top-down dfs
                parent_call = path[parent_node_id][0]
                parent_call_id = self.decoder_api_dict.get_node_val_from_callMap(parent_call)

                if not (curr_node_id is None or parent_call_id is None):
                    parsed_data_array.append((parent_call_id, edge_type, curr_node_id))

            data_points.append((evidences, parsed_data_array))



            done += 1
            if done % 100000 == 0:
                print('Extracted data for {} programs'.format(done), end='\n')
                # break

        print('{:8d} programs/asts in training data'.format(done))

        # randomly shuffle to avoid bias towards initial data points during training
        random.shuffle(data_points)
        evidences, parsed_data_array = zip(*data_points) #unzip

        return evidences, parsed_data_array






class decoderDict():


        def __init__(self, infer, pre_loaded_vocab=None):
            self.infer = infer
            if not infer:
                self.call_dict = dict()
                self.call_dict['STOP'] = 0
                self.call_count = 1
            else:
                self.call_dict = pre_loaded_vocab.vocab
                self.call_count = pre_loaded_vocab.vocab_size



        def get_or_add_node_val_from_callMap(self, nodeVal):
            if (self.infer) and (nodeVal not in self.call_dict):
                return None
            elif (self.infer) or (nodeVal in self.call_dict):
                return self.call_dict[nodeVal]
            else:
                nextOpenPos = self.call_count
                self.call_dict[nodeVal] = nextOpenPos
                self.call_count += 1
                return nextOpenPos

        def get_node_val_from_callMap(self, nodeVal):
            if (self.infer) and (nodeVal not in self.call_dict):
                return None
            else:
                return self.call_dict[nodeVal]

        def get_call_dict(self):
            return self.call_dict, self.call_count
