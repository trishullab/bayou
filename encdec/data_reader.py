import os
import sys
import json
import random
import itertools

sys.path.append(os.path.abspath(os.path.join('..', '')))
from dsl import *

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

def blowup_and_sample(seqs):
    sub_seqs = []
    for seq in seqs:
        cuts = [seq[:i] for i in range(1, len(seq)+1)]
        for s in cuts:
            if s not in sub_seqs:
                sub_seqs += [s]
    if len(sub_seqs) > 15: # to prevent memory hog in powerset calculation below
        assert False
    powerset = list(itertools.chain.from_iterable(itertools.combinations(sub_seqs, r) for r in
                    range(1, len(sub_seqs) + 1)))
    random.shuffle(powerset)
    samples = powerset[:10] # some number of samples
    return samples

def read_data(filename, args):
    with open(filename) as f:
        js = json.loads(f.read())
    inputs, targets = [], []
    ignored, done = 0, 0

    for program in js['programs']:
        seqs = get_seqs(program['sequences'])
        ast_paths = get_ast_paths(program['ast'])
        try:
            samples = blowup_and_sample(seqs)
            for path in ast_paths:
                assert len(path) <= args.max_ast_depth
            for sub_set in samples:
                subset = list(sub_set)
                assert len(subset) <= args.max_seqs
                for seq in subset:
                    assert len(seq) <= args.max_seq_length
                sorted_s = sorted(subset)
                for path in ast_paths:
                    inputs.append(sorted_s)
                    targets.append(path)
        except AssertionError:
            ignored += 1
        done += 1
        print('{:8d} programs done'.format(done), end='\r')

    print('\n{:8d} programs ignored'.format(ignored))
    return inputs, targets

if __name__ == '__main__':
    print_data(sys.argv[1])
