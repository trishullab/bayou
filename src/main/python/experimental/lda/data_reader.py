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

import os
import sys
import json
import itertools

sys.path.append(os.path.abspath(os.path.join('..', '')))
from dsl import *

def get_paths(js, topic):
    node = js['node']
    assert node in ast_map, 'Unrecognized AST node: {:s}'.format(node)
    if ast_map[node] == []:
        return [[(node, LEAF_EDGE, topic)]]
    lst = []
    for child in ast_map[node]:
        if type(child) is list:
            child, nt = child[0]
            for child_node in js[child]:
                lst.append((child_node, nt))
            lst.append(STOP)
        else:
            lst.append((js[child[0]], child[1]))
    children_paths = [get_paths(child, topic) if nt and child is not None else [[(child, LEAF_EDGE, topic)]] for child, nt in lst]
    prefix = [(node, CHILD_EDGE, topic)]
    paths = []
    for i, child_paths in enumerate(children_paths):
        paths += [prefix + child_path for child_path in child_paths]
        child, nt = lst[i][0], lst[i][1]
        prefix += [(child['node'] if nt and child is not None else child, SIBLING_EDGE, topic)]
    return paths

def print_data(filename):
    [print(path) for path in read_data(filename)]

def read_data(filename):
    with open(filename) as f:
        js = json.loads(f.read())
    if 'programs' in js:
        data = [get_paths(program['ast'], topic) for program in js['programs'] for topic in program['topic']]
        return itertools.chain.from_iterable(data)
    return get_paths(js['ast'], js['topic'])

if __name__ == '__main__':
    print_data(sys.argv[1])
