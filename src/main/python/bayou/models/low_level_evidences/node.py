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

from graphviz import Digraph

CHILD_EDGE = True
SIBLING_EDGE = False


class Node():
    def __init__(self, call, child=None, sibling=None):
        self.val = call
        self.child = child
        self.sibling = sibling

    def addAndProgressSiblingNode(self, predictionNode):
        self.sibling = predictionNode
        return self.sibling

    def addAndProgressChildNode(self, predictionNode):
        self.child = predictionNode
        return self.child


    def bfs(self):

        buffer = []
        stack = []
        bfs_id = None
        parent_id = 0
        if self is not None:
            stack.append((self, parent_id, SIBLING_EDGE))
            bfs_id = 0


        while( len(stack) > 0 ):

            item_triple = stack.pop()
            item  = item_triple[0]
            parent_id = item_triple[1]
            edge_type = item_triple[2]

            buffer.append((item.val, parent_id, edge_type))


            if item.sibling is not None:
                stack.append((item.sibling, bfs_id , SIBLING_EDGE))

            if item.child is not None:
                stack.append((item.child, bfs_id, CHILD_EDGE))


            bfs_id += 1

        return buffer



    def dfs(self):

        buffer = []
        stack = []
        dfs_id = None
        parent_id = 0
        if self is not None:
            stack.append((self, parent_id, SIBLING_EDGE))
            dfs_id = 0

        while( len(stack) > 0 ):

            item_triple = stack.pop()
            item  = item_triple[0]
            parent_id = item_triple[1]
            edge_type = item_triple[2]

            buffer.append((item.val, parent_id, edge_type))

            if item.child is not None:
                stack.append((item.child, dfs_id, CHILD_EDGE))

            if item.sibling is not None:
                stack.append((item.sibling, dfs_id , SIBLING_EDGE))

            dfs_id += 1

        return buffer

    def iterateHTillEnd(self):
        head = self
        while(head.sibling != None):
            head = head.sibling
        return head



def get_ast(js, idx=0):
     #print (idx)
     cons_calls = []
     i = idx
     curr_Node = Node("DSubTree")
     head = curr_Node
     while i < len(js):
         if js[i]['node'] == 'DAPICall':
             curr_Node.sibling = Node(js[i]['_call'])
             curr_Node = curr_Node.sibling
         else:
             break
         i += 1
     if i == len(js):
         curr_Node.sibling = Node('STOP')
         curr_Node = curr_Node.sibling
         return head

     node_type = js[i]['node']

     if node_type == 'DBranch':

         nodeC = read_DBranch(js[i])

         future = get_ast(js, i+1)
         future = future.sibling


         branching = Node('DBranch', child=nodeC, sibling=future)
         curr_Node.sibling = branching
         curr_Node = curr_Node.sibling
         return head

     if node_type == 'DExcept':

         nodeT = read_DExcept(js[i])

         future = get_ast(js, i+1)
         future = future.sibling

         exception = Node('DExcept' , child=nodeT, sibling=future)
         curr_Node.sibling = exception
         curr_Node = curr_Node.sibling
         return head


     if node_type == 'DLoop':

         nodeC = read_DLoop(js[i])

         future = get_ast(js, i+1)
         future = future.sibling

         loop = Node('DLoop', child=nodeC, sibling=future)
         curr_Node.sibling = loop
         curr_Node = curr_Node.sibling

         return head


def read_DLoop(js_branch):
     nodeC = get_ast(js_branch['_cond'])  # will have at most 1 "path"
     nodeC = nodeC.sibling
     # assert len(pC) <= 1

     nodeB  = get_ast(js_branch['_body'])
     nodeB = nodeB.sibling

     nodeC.child = nodeB
     return nodeC


def read_DExcept(js_branch):

     nodeT = get_ast(js_branch['_try'])
     nodeT = nodeT.sibling
     nodeC = get_ast(js_branch['_catch'])
     nodeC = nodeC.sibling

     nodeT.child = nodeC

     return nodeT


def read_DBranch(js_branch):

     nodeC = get_ast(js_branch['_cond'])  # will have at most 1 "path"
     nodeC = nodeC.sibling
     # assert len(pC) <= 1

     nodeT = get_ast(js_branch['_then'])
     nodeT = nodeT.sibling
     #nodeC.child = nodeT
     nodeC.sibling = nodeT

     nodeE = get_ast(js_branch['_else'])
     nodeE = nodeE.sibling
     nodeC.child = nodeE

     return nodeC



def plot_path(i, path):
    dot = Digraph(comment='Program AST', format='eps')
    for dfs_id, item in enumerate(path):
        node_value , parent_id , edge_type = item
        dot.node( str(dfs_id) , node_value )
        label = 'child' if edge_type else 'sibling'
        label += " / " + str(dfs_id)
        if dfs_id > 0:
            dot.edge( str(parent_id) , str(dfs_id), label=label, constraint='true', direction='LR')
    dot.render('plots/' + 'program-ast-' + str(i) + '.gv')
    return dot








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

js4 =  {
    "ast": {
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



for i, _js in enumerate([js, js1, js2, js3, js4]):
     ast = get_ast(_js['ast']['_nodes'])
     path = ast.bfs()
     dot = plot_path(i, path)
     print(path)
