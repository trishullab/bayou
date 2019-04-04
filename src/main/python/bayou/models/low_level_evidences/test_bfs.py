
CHILD_EDGE = True
SIBLING_EDGE = False






class Node():
    def __init__(self, call, child=None, sibling=None):
        self.val = call
        self.child = child
        self.sibling = sibling


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



e = Node('E')
d = Node('D', sibling=e)

c = Node('C')
b = Node('B', child=d, sibling=c)

a = Node('A', child=b )


print(a.bfs())




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
       },
       {
         "node": "DAPICall",
         "_call": "java.nio.Buffer.limit(int)"
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
