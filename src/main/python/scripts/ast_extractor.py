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


CHILD_EDGE = 'V'
SIBLING_EDGE = 'H'


class Node():
    def __init__(self, call, child=None, sibling=None):
        self.val = call
        self.child = child
        self.sibling = sibling


    def iterateHTillEnd(self, head):
        while(head.sibling != None):
            head = head.sibling
        return head



class TooLongPathError(Exception):
    pass


class InvalidSketchError(Exception):
    pass


def get_ast_paths(js, idx=0):
    #print (idx)
    cons_calls = []
    i = idx
    curr_Node = None
    head = None
    while i < len(js):
        if js[i]['node'] == 'DAPICall':
            cons_calls.append((js[i]['_call'], SIBLING_EDGE))
            if curr_Node == None:
                curr_Node = Node(js[i]['_call'])
                head = curr_Node
            else:
                curr_Node.sibling = Node(js[i]['_call'])
                curr_Node = curr_Node.sibling
        else:
            break
        i += 1
    if i == len(js):
        cons_calls.append(('STOP', SIBLING_EDGE))
        if curr_Node == None:
            curr_Node = Node('STOP')
            head = curr_Node
        else:
            curr_Node.sibling = Node('STOP')
            curr_Node = curr_Node.sibling
        return head, [cons_calls]

    node_type = js[i]['node']

    if node_type == 'DBranch':

        if curr_Node == None:
            curr_Node = Node('DBranch')
            head = curr_Node
        else:
            curr_Node.sibling = Node('DBranch')
            curr_Node = curr_Node.sibling

        nodeC, pC = get_ast_paths( js[i]['_cond'])  # will have at most 1 "path"
        assert len(pC) <= 1
        nodeC_last = nodeC.iterateHTillEnd(nodeC)
        nodeC_last.sibling, p1 = get_ast_paths( js[i]['_then'])
        nodeE, p2 = get_ast_paths( js[i]['_else'])
        curr_Node.child = Node(nodeC.val, child=nodeE, sibling=nodeC.sibling)

        p = [p1[0] + path for path in p2] + p1[1:]
        pv = [cons_calls + [('DBranch', CHILD_EDGE)] + pC[0] + path for path in p]


        nodeS, p = get_ast_paths( js, i+1)
        ph = [cons_calls + [('DBranch', SIBLING_EDGE)] + path for path in p]
        curr_Node.sibling = nodeS

        return head, ph + pv

    if node_type == 'DExcept':
        if curr_Node == None:
            curr_Node = Node('DExcept')
            head = curr_Node
        else:
            curr_Node.sibling = Node('DExcept')
            curr_Node = curr_Node.sibling

        nodeT , p1 = get_ast_paths( js[i]['_try'])
        nodeC , p2 =  get_ast_paths( js[i]['_catch'] )
        p = [p1[0] + path for path in p2] + p1[1:]

        curr_Node.child = Node(nodeT.val, child=nodeC, sibling=nodeT.sibling)
        pv = [cons_calls + [('DExcept', CHILD_EDGE)] + path for path in p]

        nodeS, p = get_ast_paths( js, i+1)
        ph = [cons_calls + [('DExcept', SIBLING_EDGE)] + path for path in p]
        curr_Node.sibling = nodeS
        return head, ph + pv

    if node_type == 'DLoop':
        if curr_Node == None:
            curr_Node = Node('DLoop')
            head = curr_Node
        else:
            curr_Node.sibling = Node('DLoop')
            curr_Node = curr_Node.sibling
        nodeC, pC = get_ast_paths( js[i]['_cond'])  # will have at most 1 "path"
        assert len(pC) <= 1
        nodeC_last = nodeC.iterateHTillEnd(nodeC)
        nodeC_last.sibling, p = get_ast_paths( js[i]['_body'])

        pv = [cons_calls + [('DLoop', CHILD_EDGE)] + pC[0] + path for path in p]
        nodeS, p = get_ast_paths( js, i+1)
        ph = [cons_calls + [('DLoop', SIBLING_EDGE)] + path for path in p]

        curr_Node.child = nodeC
        curr_Node.sibling = nodeS

        return head, ph + pv


def _check_DAPICall_repeats(nodelist):
    """
    Checks if an API call node repeats in succession twice in a list of nodes

    :param nodelist: list of nodes to check
    :return: None
    :raise: InvalidSketchError if some API call node repeats, ValueError if a node is of invalid type
    """
    for i in range(1, len(nodelist)):
        node = nodelist[i]
        node_type = node['node']
        if node_type == 'DAPICall':
            if nodelist[i] == nodelist[i-1]:
                raise InvalidSketchError
        elif node_type == 'DBranch':
            _check_DAPICall_repeats(node['_cond'])
            _check_DAPICall_repeats(node['_then'])
            _check_DAPICall_repeats(node['_else'])
        elif node_type == 'DExcept':
            _check_DAPICall_repeats(node['_try'])
            _check_DAPICall_repeats(node['_catch'])
        elif node_type == 'DLoop':
            _check_DAPICall_repeats(node['_cond'])
            _check_DAPICall_repeats(node['_body'])
        else:
            raise ValueError('Invalid node type: ' + node)

def validate_sketch_paths(program, ast_paths, max_ast_depth):
    """
    Checks if a sketch along with its paths is good training data:
    1. No API call should be repeated successively
    2. No path in the sketch should be of length more than max_ast_depth hyper-parameter
    3. No branch, loop or except should occur more than once along a single path

    :param program: the sketch
    :param ast_paths: paths in the sketch
    :return: None
    :raise: TooLongPathError or InvalidSketchError if sketch or its paths is invalid
    """
    _check_DAPICall_repeats(program['ast']['_nodes'])
    for path in ast_paths:
        if len(path) >= max_ast_depth:
            raise TooLongPathError
        nodes = [node for (node, edge) in path]
        if nodes.count('DBranch') > 1 or nodes.count('DLoop') > 1 or nodes.count('DExcept') > 1:
            raise TooLongPathError

