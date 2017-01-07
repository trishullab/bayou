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
        'DName'                         : [ ('type', False) ],
        'DNullLiteral'                  : [ ],
        'DParenthesizedExpression'      : [ ('expression',True) ],
        'DTryStatement'                 : [ ('tryBlock',True), [('catchClauses',True)] ],
        'DVariableDeclarationFragment'  : [ ('name',True), ('initializer',True) ],
        'DVariableDeclarationStatement' : [ [('fragments',True)] ],
        'DWhileStatement'               : [ ('cond',True), ('body',True) ]
        }
# These nodes can have an API call
call_nodes = [ 'DClassInstanceCreation', 'DMethodInvocation' ]

STOP = ('STOP',False)

# Type of edge to the next node along the path. Note: LEAF_EDGE is redundant since it's always 
# the last edge along a path, but it's there to keep the data format consistent.
CHILD_EDGE, SIBLING_EDGE, LEAF_EDGE = 'V', 'H', 'L'
