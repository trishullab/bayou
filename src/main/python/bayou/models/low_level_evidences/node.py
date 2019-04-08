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


class Node():
    def __init__(self, call, child=None, sibling=None):
        self.val = call
        self.child = child
        self.sibling = sibling




    def bfs(self):


        buffer = [('DSubTree', None, SIBLING_EDGE)]
        queue = []
        bfs_id = 1
        parent_id = 0
        if self is not None:
            queue.insert(0, (self, parent_id, SIBLING_EDGE))


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

        buffer = [('DSubTree', None, SIBLING_EDGE)]
        queue = []
        bfs_id = 1
        parent_id = 0
        if self is not None:
            queue.append((self, parent_id, SIBLING_EDGE))

        while( len(queue) > 0 ):

            item_triple = queue.pop()
            item  = item_triple[0]
            parent_id = item_triple[1]
            edge_type = item_triple[2]

            buffer.append((item.val, parent_id, edge_type))

            if item.child is not None:
                queue.append((item.child, bfs_id, CHILD_EDGE))

            if item.sibling is not None:
                queue.append((item.sibling, bfs_id , SIBLING_EDGE))

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


         nodeC = get_ast(js[i]['_cond'])  # will have at most 1 "path"
         nodeC = nodeC.sibling
         # assert len(pC) <= 1

         nodeT = get_ast(js[i]['_then'])
         nodeT = nodeT.sibling
         #nodeC.child = nodeT
         nodeC.sibling = nodeT

         nodeE = get_ast(js[i]['_else'])
         nodeE = nodeE.sibling
         nodeC.child = nodeE

         future = get_ast(js, i+1)
         future = future.sibling


         branching = Node('DBranch', child=nodeC, sibling=future)
         curr_Node.sibling = branching
         curr_Node = curr_Node.sibling
         return head

     if node_type == 'DExcept':
         # curr_Node.child = Node('DExcept')
         # curr_Node = curr_Node.child

         nodeT = get_ast(js[i]['_try'])
         nodeT = nodeT.sibling
         nodeC = get_ast(js[i]['_catch'])
         nodeC = nodeC.sibling

         nodeT.child = nodeC

         future = get_ast(js, i+1)
         future = future.sibling
         exception = Node('DExcept' , child=nodeT, sibling=future)
         curr_Node.sibling = exception
         curr_Node = curr_Node.sibling
         return head


     if node_type == 'DLoop':

         nodeC = get_ast(js[i]['_cond'])  # will have at most 1 "path"
         nodeC = nodeC.sibling
         # assert len(pC) <= 1

         nodeB  = get_ast(js[i]['_body'])
         nodeB = nodeB.sibling

         nodeC.child = nodeB

         future = get_ast(js, i+1)
         future = future.sibling
         loop = Node('DLoop', child=nodeC, sibling=future)
         curr_Node.sibling = loop
         curr_Node = curr_Node.sibling

         return head











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



# for _js in [js, js1, js2, js3, js4]:
#      ast = get_ast(_js['ast']['_nodes'])
#      print(ast.dfs())
#      print()
