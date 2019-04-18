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

MAX_LOOP_NUM = 3
MAX_BRANCHING_NUM = 3


class TooLongLoopingException(Exception):
    pass


class TooLongBranchingException(Exception):
    pass


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


    def check_nested_branch(self):
        head = self
        count = 0
        while(head != None):
            if head.val == 'DBranch':
                count_Else = head.child.child.check_nested_branch() #else
                count_Then = head.child.sibling.check_nested_branch() #then
                count = 1 + max(count_Then, count_Else)
                if count > MAX_BRANCHING_NUM:
                    raise TooLongBranchingException
            head = head.sibling
        return count

    def check_nested_loop(self):
        head = self
        count = 0
        while(head != None):
            if head.val == 'DLoop':
                count = 1 + head.child.child.check_nested_loop()

                if count > MAX_LOOP_NUM:
                    raise TooLongLoopingException
            head = head.sibling
        return count



    def depth_first_search(self):

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


            if item.sibling is not None:
                stack.append((item.sibling, dfs_id , SIBLING_EDGE))

            if item.child is not None:
                stack.append((item.child, dfs_id, CHILD_EDGE))



            dfs_id += 1

        return buffer

    def iterateHTillEnd(self):
        head = self
        while(head.sibling != None):
            head = head.sibling
        return head



def get_ast_from_json(js):
    ast = get_ast(js, idx=0)
    real_head = Node("DSubTree")
    real_head.sibling = ast
    return real_head


def get_ast(js, idx=0):
     #print (idx)
     cons_calls = []
     i = idx
     curr_Node = Node("Dummy_Fist_Sibling")
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
         return head.sibling

     node_type = js[i]['node']

     if node_type == 'DBranch':

         nodeC = read_DBranch(js[i])

         future = get_ast(js, i+1)
         branching = Node('DBranch', child=nodeC, sibling=future)

         curr_Node.sibling = branching
         curr_Node = curr_Node.sibling
         return head.sibling

     if node_type == 'DExcept':

         nodeT = read_DExcept(js[i])

         future = get_ast(js, i+1)

         exception = Node('DExcept' , child=nodeT, sibling=future)
         curr_Node.sibling = exception
         curr_Node = curr_Node.sibling
         return head.sibling


     if node_type == 'DLoop':

         nodeC = read_DLoop(js[i])
         future = get_ast(js, i+1)

         loop = Node('DLoop', child=nodeC, sibling=future)
         curr_Node.sibling = loop
         curr_Node = curr_Node.sibling

         return head.sibling


def read_DLoop(js_branch):
    # assert len(pC) <= 1
     nodeC = get_ast(js_branch['_cond'])  # will have at most 1 "path"
     nodeB  = get_ast(js_branch['_body'])
     nodeC.child = nodeB

     return nodeC


def read_DExcept(js_branch):

     nodeT = get_ast(js_branch['_try'])
     nodeC = get_ast(js_branch['_catch'])

     dummyNode = Node('STOP')

     dummyNode.child = nodeT
     dummyNode.sibling = nodeC

     return dummyNode


def read_DBranch(js_branch):

     nodeC = get_ast(js_branch['_cond'])  # will have at most 1 "path"
     # assert len(pC) <= 1
     nodeT = get_ast(js_branch['_then'])
     #nodeC.child = nodeT
     nodeE = get_ast(js_branch['_else'])

     nodeC.sibling = nodeE
     nodeC.child = nodeT

     return nodeC


def colnum_string(n):
    n = n + 26*26*26 +26*26 +26 +1
    string = ""
    while n > 0:
        n, remainder = divmod(n - 1, 26)
        string = chr(65 + remainder) + string
    return string


def plot_path(i, path, prob):
    dot = Digraph(comment='Program AST', format='eps')
    dot.node(str(prob), str(prob)[:6])
    for dfs_id, item in enumerate(path):
        node_value , parent_id , edge_type = item
        dot.node( str(dfs_id) , node_value )
        label = 'child' if edge_type else 'sibling'
        label += " / " + str(dfs_id)
        if dfs_id > 0:
            dot.edge( str(parent_id) , str(dfs_id), label=label, constraint='true', direction='LR')

    stri = colnum_string(i)
    dot.render('plots/' + 'program-ast-' + stri + '.gv')
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
          },
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

if __name__ == "__main__":
    for i, _js in enumerate([js, js1, js2, js3, js4]):
         ast = get_ast_from_json(_js['ast']['_nodes'])
         path = ast.depth_first_search()
         dot = plot_path(i, path, 0.0)
         print(path)
