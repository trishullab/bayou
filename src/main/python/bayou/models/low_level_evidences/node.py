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


# from bayou.models.low_level_evidences.utils import CHILD_EDGE, SIBLING_EDGE

CHILD_EDGE = True
SIBLING_EDGE = False

from copy import deepcopy

class Node():
    def __init__(self, call, child=None, sibling=None):
        self.val = call
        self.child = child
        self.sibling = sibling



    def dfs(self, inp=False, buffer=[]):
        buffer.append((self.val, inp))

        if self.child is not None:
            self.child.dfs(inp=True, buffer=buffer)

        if self.sibling is not None:
            self.sibling.dfs(inp=False, buffer=buffer)

        return buffer

    def bfs(self):


        buffer = [('DSubTree', None, CHILD_EDGE)]
        queue = []
        bfs_id = 1
        parent_id = 0
        if self is not None:
            queue.insert(0, (self, parent_id, CHILD_EDGE))


        while( len(queue) > 0 ):

            item_triple = queue.pop()
            item  = item_triple[0]
            parent_id = item_triple[1]
            edge_type = item_triple[2]

            buffer.append((item.val, parent_id, edge_type))


            if item.sibling is not None:
                queue.insert(0, (item.sibling, bfs_id , SIBLING_EDGE))

            if item.child is not None:
                queue.insert(0, (item.child, bfs_id, CHILD_EDGE))


            bfs_id += 1

        return buffer



    def dfs(self):

        buffer = [('DSubTree', None, CHILD_EDGE)]
        queue = []
        bfs_id = 1
        parent_id = 0
        if self is not None:
            queue.append((self, parent_id, CHILD_EDGE))

        while( len(queue) > 0 ):

            item_triple = queue.pop()
            item  = item_triple[0]
            parent_id = item_triple[1]
            edge_type = item_triple[2]

            buffer.append((item.val, parent_id, edge_type))

            if item.sibling is not None:
                queue.append((item.sibling, bfs_id , SIBLING_EDGE))

            if item.child is not None:
                queue.append((item.child, bfs_id, CHILD_EDGE))


            bfs_id += 1

        return buffer

    def iterateHTillEnd(self):
        head = self
        while(head.child != None):
            head = head.child
        return head



def get_ast(js, idx=0):
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

         nodeC = get_ast(js[i]['_cond'])  # will have at most 1 "path"
         nodeC = nodeC.child
         # assert len(pC) <= 1
         curr_Node.child = nodeC
         # curr_Node = nodeC.iterateHTillEnd(nodeC)
         nodeT = get_ast(js[i]['_then'])
         nodeT = nodeT.child
         curr_Node.child.sibling = nodeT

         nodeE = get_ast(js[i]['_else'])
         nodeE = nodeE.child
         curr_Node.child.sibling.sibling = nodeE

         future = get_ast(js, i+1)
         future = future.child
         curr_Node.child.child = future

         return head

     if node_type == 'DExcept':
         # curr_Node.child = Node('DExcept')
         # curr_Node = curr_Node.child

         nodeT = get_ast(js[i]['_try'])
         nodeT = nodeT.child
         nodeC = get_ast(js[i]['_catch'])
         nodeC = nodeC.child

         curr_Node.child = nodeT #Node(nodeT.val, sibling=nodeT.sibling, child=nodeC)
         curr_Node.child.sibling = nodeC #curr_Node.iterateHTillEnd()

         future = get_ast(js, i+1)
         future = future.child
         curr_Node.child.child = future
         return head


     if node_type == 'DLoop':

         nodeC = get_ast(js[i]['_cond'])  # will have at most 1 "path"
         nodeC = nodeC.child
         # assert len(pC) <= 1

         nodeB  = get_ast(js[i]['_body'])
         nodeB = nodeB.child

         curr_Node.child = nodeC
         curr_Node.child.sibling = nodeB
         curr_Node.child.sibling.sibling = deepcopy(nodeB)
         curr_Node.child.sibling.sibling.sibling = deepcopy(nodeB)

         future = get_ast(js, i+1)
         future = future.child
         curr_Node.child.child = future

         return head









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


def validate_sketch_paths( program, ast_paths):
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
     if len(path) >= 32:
         raise TooLongPathError
     nodes = [node for (node, edge) in path]
     if nodes.count('DBranch') > 1 or nodes.count('DLoop') > 1 or nodes.count('DExcept') > 1:
         raise TooLongPathError

class TooLongPathError(Exception):
     pass


class InvalidSketchError(Exception):
     pass



js = {
   "ast": {
     "node": "DSubTree",
     "_nodes": [
       {
         "node": "DBranch",
         "_cond": [
           {
             "node": "DAPICall",
             "_call": "java.nio.Buffer.capacity()"
           }
         ],
         "_else": [
           {
             "node": "DAPICall",
             "_call": "java.nio.ByteBuffer.allocate(int)"
           },
           {
             "node": "DAPICall",
             "_call": "java.nio.ByteBuffer.allocate(int)"
           }
         ],
         "_then": [
           {
             "node": "DAPICall",
             "_call": "java.nio.Buffer.clear()"
           }
         ]
       },
       {
         "node": "DAPICall",
         "_call": "java.nio.ByteBuffer.array()"
       },
       {
         "node": "DAPICall",
         "_call": "java.nio.Buffer.position()"
       },
       {
         "node": "DAPICall",
         "_call": "java.lang.System.arraycopy(java.lang.Object,int,java.lang.Object,int,int)"
       },
       {
         "node": "DAPICall",
         "_call": "java.lang.System.arraycopy(java.lang.Object,int,java.lang.Object,int,int)"
       },
       {
         "node": "DAPICall",
         "_call": "java.nio.Buffer.limit(int)"
       }
     ]
   }
 }

js1 = {
   "ast": {
     "node": "DSubTree",
     "_nodes": [
       {
         "node": "DAPICall",
         "_call": "java.io.File.delete()"
       },
       {
         "node": "DLoop",
         "_cond": [
           {
             "node": "DAPICall",
             "_call": "java.util.Iterator<Tau_E>.hasNext()"
           }
         ],
         "_body": [
           {
             "node": "DAPICall",
             "_call": "java.util.Iterator<Tau_E>.next()"
           }
         ]
       }
     ]
   }
 }

js2 = {
    "ast": {
     "node": "DSubTree",
     "_nodes": [
       {
         "node": "DExcept",
         "_catch": [
           {
             "node": "DAPICall",
             "_call": "java.lang.Throwable.getMessage()"
           }
         ],
         "_try": [
           {
             "node": "DAPICall",
             "_call": "java.lang.Class<Tau_T>.getResource(java.lang.String)"
           },
           {
             "node": "DAPICall",
             "_call": "java.net.URL.openStream()"
           }
         ]
       }
     ]
   }
 }





js3 = {
    "ast": {
        "_nodes": [
          {
            "_try": [
              {
                "_throws": [
                  "java.security.NoSuchAlgorithmException"
                ],
                "_call": "java.security.SecureRandom.getInstance(java.lang.String)",
                "node": "DAPICall",
                "_returns": "java.security.SecureRandom"
              }
            ],
            "_catch": [
              {
                "_throws": [],
                "_call": "java.lang.Throwable.printStackTrace()",
                "node": "DAPICall",
                "_returns": "void"
              }
            ],
            "node": "DExcept"
          }
        ],
        "node": "DSubTree"
      }
}

js4 =  {"ast": {
        "_nodes": [
          {
            "_cond": [],
            "node": "DBranch",
            "_then": [
              {
                "_throws": [
                  "java.awt.HeadlessException"
                ],
                "_call": "javax.swing.JOptionPane.showMessageDialog(java.awt.Component,java.lang.Object)",
                "node": "DAPICall",
                "_returns": "void"
              }
            ],
            "_else": [
              {
                "_throws": [
                  "java.awt.HeadlessException"
                ],
                "_call": "javax.swing.JOptionPane.showMessageDialog(java.awt.Component,java.lang.Object)",
                "node": "DAPICall",
                "_returns": "void"
              }
            ]
          }
        ],
        "node": "DSubTree"
      }
}


ast = get_ast(js['ast']['_nodes'])



# print(ast.dfs())
