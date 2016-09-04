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
        'DInfixExpression'              : [ ('left',True), ('right',True), ('operator',False) ],
        'DMethodInvocation'             : [ ('methodName',False) ],
        'DName'                         : [ ],
        'DNullLiteral'                  : [ ],
        'DParenthesizedExpression'      : [ ('expression',True) ],
        'DTryStatement'                 : [ ('tryBlock',True), [('catchClauses',True)], ('finallyBlock',True) ],
        'DVariableDeclarationFragment'  : [ ('name',True), ('initializer',True) ],
        'DVariableDeclarationStatement' : [ [('fragments',True)] ],
        'DWhileStatement'               : [ ('cond',True), ('body',True) ]
        }
STOP = ('STOP',False)

# Type of edge to the next node along the path. Note: LEAF_EDGE is redundant since it's always the last edge
# along a path, but it's there to keep the data format consistent.
CHILD_EDGE, SIBLING_EDGE, LEAF_EDGE = 'V', 'H', 'L'

def get_paths(js):
    node = js['node']
    assert node in ast_map, 'Unrecognized AST node: {:s}'.format(node)
    if ast_map[node] == []:
        return [[(node, LEAF_EDGE)]]
    lst = []
    for child in ast_map[node]:
        if type(child) is list:
            child, nt = child[0]
            if child in js:
                for child_node in js[child]:
                    lst.append((child_node, nt))
                lst.append(STOP)
        else:
            if child[0] in js:
                lst.append((js[child[0]], child[1]))
    children_paths = [get_paths(child) if nt else [[(child, LEAF_EDGE)]] for child, nt in lst]
    prefix = [(node, CHILD_EDGE)]
    paths = []
    for i, child_paths in enumerate(children_paths):
        paths += [prefix + child_path for child_path in child_paths]
        prefix += [(lst[i][0]['node'] if lst[i][1] else lst[i][0], SIBLING_EDGE)]
    return paths

def print_data(filename):
    [print(path) for path in read_data(filename)]

def read_data(filename):
    with open(filename) as f:
        js = json.loads(f.read())
    if 'asts' in js:
        data = [get_paths(ast) for ast in js['asts']]
        return itertools.chain.from_iterable(data)
    return get_paths(js)

if __name__ == '__main__':
    print_data(sys.argv[1])
