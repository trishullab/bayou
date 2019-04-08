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
from bayou.models.low_level_evidences.node import Node, CHILD_EDGE, SIBLING_EDGE



class Reader():
    def __init__(self, clargs, config, infer=False, dataIsThere=False):
        self.infer = infer
        self.config = config

        if clargs.continue_from is not None or dataIsThere:
            with open('data/inputs.txt', 'rb') as f:
                self.inputs = pickle.load(f)
            with open('data/nodes.txt', 'rb') as f:
                self.nodes = pickle.load(f)
            with open('data/parents.txt', 'rb') as f:
                self.parents = pickle.load(f)
            with open('data/edges.txt', 'rb') as f:
                self.edges = pickle.load(f)
            with open('data/targets.txt', 'rb') as f:
                self.targets = pickle.load(f)

            jsconfig = dump_config(config)
            with open(os.path.join(clargs.save, 'config.json'), 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)

            if infer:
                self.js_programs = []
                with open('data/js_programs.json', 'rb') as f:
                    for program in ijson.items(f, 'programs.item'):
                        self.js_programs.append(program)
            config.num_batches = int(len(self.nodes) / config.batch_size)

        else:
            random.seed(12)
            # read the raw evidences and targets
            print('Reading data file...')
            raw_evidences, raw_targets, js_programs = self.read_data(clargs.input_file[0], infer, save=clargs.save)
            print('Done!')
            raw_evidences = [[raw_evidence[i] for raw_evidence in raw_evidences] for i, ev in
                             enumerate(config.evidence)]


            config.num_batches = int(len(raw_targets) / config.batch_size)
            assert config.num_batches > 0, 'Not enough data'

            sz = config.num_batches * config.batch_size
            for i in range(len(raw_evidences)):
                raw_evidences[i] = raw_evidences[i][:sz]
            raw_targets = raw_targets[:sz]

        # setup input and target chars/vocab
            if clargs.continue_from is None:
                config.decoder.vocab = self.CallMapDict
                config.decoder.vocab_size = len(self.CallMapDict)
                # adding the same variables for reverse Encoder

            # wrangle the evidences and targets into numpy arrays
            self.inputs = [ev.wrangle(data) for ev, data in zip(config.evidence, raw_evidences)]
            self.nodes = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)
            self.parents = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)
            self.edges = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.bool)
            self.targets = np.zeros((sz, config.decoder.max_ast_depth), dtype=np.int32)

            for i, path in enumerate(raw_targets):
                len_path = min(len(path) , config.decoder.max_ast_depth)
                mod_path = path[:len_path]
                
                self.nodes[i, :len_path] = [p[0] for p in mod_path]
                self.parents[i, :len_path] = [p[1] for p in mod_path]
                self.edges[i, :len_path] = [p[2] for p in mod_path]
                self.targets[i, :len_path-1] = self.nodes[i, 1:len(mod_path)]  # shifted left by one

            self.js_programs = js_programs


            with open('data/inputs.txt', 'wb') as f:
                pickle.dump(self.inputs, f)
            with open('data/nodes.txt', 'wb') as f:
                pickle.dump(self.nodes, f)
            with open('data/edges.txt', 'wb') as f:
                pickle.dump(self.edges, f)
            with open('data/parents.txt', 'wb') as f:
                pickle.dump(self.parents, f)
            with open('data/targets.txt', 'wb') as f:
                pickle.dump(self.targets, f)
            with open('data/js_programs.json', 'w') as f:
                json.dump({'programs': self.js_programs}, fp=f, indent=2)
            jsconfig = dump_config(config)
            with open(os.path.join(clargs.save, 'config.json'), 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)
            with open('data/config.json', 'w') as f:
                json.dump(jsconfig, fp=f, indent=2)



    def get_ast(self, js, idx=0):
         #print (idx)
         cons_calls = []
         i = idx
         curr_Node = Node("DSubTree")
         head = curr_Node
         while i < len(js):
             if js[i]['node'] == 'DAPICall':
                 curr_Node.child = Node(js[i]['_call'])
                 curr_Node = curr_Node.child
             else:
                 break
             i += 1
         if i == len(js):
             curr_Node.child = Node('STOP')
             curr_Node = curr_Node.child
             return head

         node_type = js[i]['node']

         if node_type == 'DBranch':

             nodeC = self.get_ast(js[i]['_cond'])  # will have at most 1 "path"
             nodeC = nodeC.child
             # assert len(pC) <= 1
             curr_Node.child = nodeC
             # curr_Node = nodeC.iterateHTillEnd(nodeC)
             nodeT = self.get_ast(js[i]['_then'])
             nodeT = nodeT.child
             curr_Node.child.sibling = nodeT

             nodeE = self.get_ast(js[i]['_else'])
             nodeE = nodeE.child
             curr_Node.child.sibling.sibling = nodeE

             future = self.get_ast(js, i+1)
             future = future.child
             curr_Node.child.child = future

             return head

         if node_type == 'DExcept':
             # curr_Node.child = Node('DExcept')
             # curr_Node = curr_Node.child

             nodeT = self.get_ast(js[i]['_try'])
             nodeT = nodeT.child
             nodeC = self.get_ast(js[i]['_catch'])
             nodeC = nodeC.child

             curr_Node.child = nodeT #Node(nodeT.val, sibling=nodeT.sibling, child=nodeC)
             curr_Node.child.sibling = nodeC #curr_Node.iterateHTillEnd()

             future = self.get_ast(js, i+1)
             future = future.child
             curr_Node.child.child = future
             return head


         if node_type == 'DLoop':

             nodeC = self.get_ast(js[i]['_cond'])  # will have at most 1 "path"
             nodeC = nodeC.child
             # assert len(pC) <= 1

             nodeB  = self.get_ast(js[i]['_body'])
             nodeB = nodeB.child

             curr_Node.child = nodeC
             curr_Node.child.sibling = nodeB
             curr_Node.child.sibling.sibling = deepcopy(nodeB)
             curr_Node.child.sibling.sibling.sibling = deepcopy(nodeB)

             future = self.get_ast(js, i+1)
             future = future.child
             curr_Node.child.child = future

             return head



    def read_data(self, filename, infer, save=None):


        f = open(filename , 'rb')

        data_points = []
        callmap = dict()
        file_ptr = dict()
        ignored, done = 0, 0
        if not infer:
            self.CallMapDict = dict()
            self.CallMapDict['STOP'] = 0
            count = 1
        else:
            self.CallMapDict = self.config.decoder.vocab
            count = self.config.decoder.vocab_size
        for program in ijson.items(f, 'programs.item'):

            evidences = [ev.read_data_point(program, infer) for ev in self.config.evidence]
            ast_node_graph = self.get_ast(program['ast']['_nodes'])

            path = ast_node_graph.dfs()[1:]
            temp_arr = []
            for val in path:
                nodeVal = val[0]
                idVal   = val[1]
                edgeVal = val[2]
                if nodeVal not in self.CallMapDict:
                    if not infer:
                        self.CallMapDict[nodeVal] = count
                        temp_arr.append((count, idVal , edgeVal))
                        count += 1
                else:
                    temp_arr.append((self.CallMapDict[nodeVal] , idVal, edgeVal))

            sample = dict()
            sample['file'] = program['file']
            # sample['method'] = program['method']
            # sample['body'] = program['body']
            data_points.append((evidences, temp_arr, sample))


            calls = gather_calls(program['ast'])
            for call in calls:
                if call['_call'] not in callmap:
                    callmap[call['_call']] = call
            #
            file_name = program['file']
            file_ptr[done - ignored] = file_name

            done += 1
            if done % 100000 == 0:
                print('Extracted data for {} programs'.format(done), end='\n')


        print('{:8d} programs/asts in training data'.format(done))
        print('{:8d} programs/asts ignored by given config'.format(ignored))
        print('{:8d} programs/asts to search over'.format(done - ignored))
        print('{:8d} data points total'.format(len(data_points)))

        # randomly shuffle to avoid bias towards initial data points during training
        random.shuffle(data_points)
        evidences, targets, js_programs = zip(*data_points) #unzip

        # save callmap if save location is given
        if not infer:
            if save is not None:
                with open(os.path.join(save, 'callmap.pkl'), 'wb') as f:
                    pickle.dump(callmap, f)

            with open(os.path.join(save, 'file_ptr.pkl'), 'wb') as f:
                pickle.dump(file_ptr, f)

        return evidences, targets, js_programs
