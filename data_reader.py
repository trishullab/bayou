import sys
import json
import itertools

# These names should correspond to the names used in the JSON input.
# If the input was generated with Gson, this corresponds to the variable names in datasyn-dsl.
# The tuples stand for (name, Is_Non_Terminal?), where name is a string.
# If the object is a list of things, it should be [(name, Is_Non_Terminal?)] to be consistent.
ast_map = {
        'DAssignment'                   : [ ('lhs',True), ('rhs',True), ('operator',False) ],
        'DBlock'                        : [ [('statements',True)] ],
        'DCatchClause'                  : [ ('body',True) ],
        'DClassInstanceCreation'        : [ ('constructor',False) ],
        'DExpressionStatement'          : [ ('expression',True) ],
        'DIfStatement'                  : [ ('cond',True), ('thenStmt',True), ('elseStmt',True) ],
        'DInfixExpression'              : [ ('left',True), ('right',True), ('operator',False) ],
        'DMethodInvocation'             : [ ('methodName',False) ],
        'DName'                         : [ ],
        'DNullLiteral'                  : [ ],
        'DParenthesizedExpression'      : [ ('expression',True) ],
        'DTryStatement'                 : [ ('tryBlock',True), [('catchClauses',True)] ],
        'DVariableDeclarationFragment'  : [ ('name',True), ('initializer',True) ],
        'DVariableDeclarationStatement' : [ [('fragments',True)] ],
        'DWhileStatement'               : [ ('cond',True), ('body',True) ]
        }
# These nodes can have an API call
call_nodes = [ ('DClassInstanceCreation','constructor'), ('DMethodInvocation','methodName') ]

STOP = ('STOP',False)

# Type of edge to the next node along the path. Note: LEAF_EDGE is redundant since it's always 
# the last edge along a path, but it's there to keep the data format consistent.
CHILD_EDGE, SIBLING_EDGE, LEAF_EDGE = 'V', 'H', 'L'

def get_ast_paths(js):
    node = js['node']
    assert node in ast_map, 'Unrecognized AST node: {:s}'.format(node)
    if ast_map[node] == []:
        return [[(node, LEAF_EDGE)]]
    lst = []
    for child in ast_map[node]:
        if type(child) is list:
            child, nt = child[0]
            for child_node in js[child]:
                lst.append((child_node, nt))
            lst.append(STOP)
        else:
            lst.append((js[child[0]], child[1]))
    children_paths = [get_ast_paths(child) if nt and child is not None else [[(child, LEAF_EDGE)]]
                            for child, nt in lst]
    prefix = [(node, CHILD_EDGE)]
    paths = []
    for i, child_paths in enumerate(children_paths):
        paths += [prefix + child_path for child_path in child_paths]
        child, nt = lst[i][0], lst[i][1]
        prefix += [(child['node'] if nt and child is not None else child, SIBLING_EDGE)]
    return paths

def get_seqs(js):
    return [sequence['calls'] for sequence in js]

def print_data(filename):
    [print(path) for path in read_data(filename)]

def read_data(filename):
    with open(filename) as f:
        js = json.loads(f.read())
    inputs, targets = [], []
    for program in js['programs']:
        seqs, ast_paths = get_seqs(program['sequences']), get_ast_paths(program['ast'])
        inp, tar = zip(*itertools.product(seqs, ast_paths))
        inputs += inp
        targets += tar
    return inputs, targets

if __name__ == '__main__':
    print_data(sys.argv[1])
