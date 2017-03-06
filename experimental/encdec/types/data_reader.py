import os
import sys
import json
import itertools

sys.path.append(os.path.abspath(os.path.join('..', '')))
from dsl import *

def get_types(js, node):
    def get_refinement_types(idx, name):
        return [ref[idx][name] for ref in js['argRefinements']] if node in call_nodes else []

    return { 'bool_exists' : get_refinement_types(0, 'exists'),
             'bool_value'  : get_refinement_types(0, 'value'),
             'num_exists'  : get_refinement_types(1, 'exists'),
             'num_value'   : get_refinement_types(1, 'value'),
             'str_exists'  : get_refinement_types(2, 'exists'),
             'str_length'  : get_refinement_types(2, 'length'),
             'str_punct'   : get_refinement_types(2, 'containsPunct') }

def ast_paths_types(js):
    node = js['node']
    assert node in ast_map, 'Unrecognized AST node: {:s}'.format(node)
    if ast_map[node] == []:
        return [[(node, LEAF_EDGE)]], [get_types(js, node)]
    lst = []
    for child in ast_map[node]:
        if type(child) is list:
            child, nt = child[0]
            for child_node in js[child]:
                lst.append((child_node, nt))
            lst.append(STOP)
        else:
            lst.append((js[child[0]], child[1]))
    children_paths_types = [ast_paths_types(child) if nt and child is not None else
                    ([[(child, LEAF_EDGE)]], [get_types(js, node)]) for child, nt in lst]
    prefix = [(node, CHILD_EDGE)]
    paths, types = [], []
    for i, (child_paths, child_types) in enumerate(children_paths_types):
        paths += [prefix + child_path for child_path in child_paths]
        child, nt = lst[i][0], lst[i][1]
        prefix += [(child['node'] if nt and child is not None else child, SIBLING_EDGE)]
        types += [child_type for child_type in child_types]
    return paths, types

def get_seqs(js):
    return [sequence['calls'] for sequence in js]

def print_data(filename):
    [print(path) for path in read_data(filename)]

def read_data(filename):
    with open(filename) as f:
        js = json.loads(f.read())
    inputs, targets_paths, targets_types = [], [], []
    for program in js['programs']:
        seqs = get_seqs(program['sequences'])
        ast_paths, ast_types = ast_paths_types(program['ast'])
        assert len(ast_paths) == len(ast_types), 'Something wrong in reading data'
        seqs = list(sorted(seqs))
        powerset = itertools.chain.from_iterable(itertools.combinations(seqs, r) for r in
                        range(len(seqs) + 1))
        for subset in powerset:
            subset = list(subset)
            if subset == []:
                continue
            for ast_path, ast_type in zip(ast_paths, ast_types):
                inputs.append(subset)
                targets_paths.append(ast_path)
                targets_types.append(ast_type)
    return inputs, targets_paths, targets_types

if __name__ == '__main__':
    print_data(sys.argv[1])
