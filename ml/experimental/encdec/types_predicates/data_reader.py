import os
import sys
import json
import itertools

sys.path.append(os.path.abspath(os.path.join('..', '')))
from dsl import ast_map, call_nodes, STOP, CHILD_EDGE, SIBLING_EDGE, LEAF_EDGE

class DataReader(object):

    def __init__(self, filename):
        self.filename = filename

    def get_types(self, js, node):
        preds = [argref[p] for argref in js['argRefinements']] if node in call_nodes else [] \
                    for p in range(self.num_predicates)]
        return preds

    def ast_paths_types(self, js):
        node = js['node']
        assert node in ast_map, 'Unrecognized AST node: {:s}'.format(node)
        if ast_map[node] == []:
            return [[(node, LEAF_EDGE)]], [self.get_types(js, node)]
        lst = []
        for child in ast_map[node]:
            if type(child) is list:
                child, nt = child[0]
                for child_node in js[child]:
                    lst.append((child_node, nt))
                lst.append(STOP)
            else:
                lst.append((js[child[0]], child[1]))
        children_paths_types = [self.ast_paths_types(child) if nt and child is not None else
                        ([[(child, LEAF_EDGE)]], [self.get_types(js, node)]) for child, nt in lst]
        prefix = [(node, CHILD_EDGE)]
        paths, types = [], []
        for i, (child_paths, child_types) in enumerate(children_paths_types):
            paths += [prefix + child_path for child_path in child_paths]
            child, nt = lst[i][0], lst[i][1]
            prefix += [(child['node'] if nt and child is not None else child, SIBLING_EDGE)]
            types += [child_type for child_type in child_types]
        return paths, types

    def read_data(self):
        with open(self.filename) as f:
            js = json.loads(f.read())
        inputs, targets_paths, targets_types = [], [], []
        self.num_predicates = js['num_predicates']
        for program in js['programs']:
            seqs = [sequence['calls'] for sequence in program['sequences']]
            ast_paths, ast_types = self.ast_paths_types(program['ast'])
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
