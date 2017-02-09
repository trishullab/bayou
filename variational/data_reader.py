import os
import sys
import json
import random
import itertools

CHILD_EDGE, SIBLING_EDGE = 'V', 'H'
def get_ast_paths(js, idx=0):
    cons_calls = []
    i = idx
    while i < len(js):
        if js[i]['node'] == 'DAPICall':
            cons_calls.append((js[i]['_call'], SIBLING_EDGE))
        else:
            break
        i += 1
    if i == len(js):
        cons_calls.append(('STOP', SIBLING_EDGE))
        return [cons_calls]
    children_paths = []
    node_type = js[i]['node']

    if node_type == 'DBranch':
        pC = get_ast_paths(js[i]['_cond']) # will have at most 1 "path"
        assert len(pC) <= 1
        p1 = get_ast_paths(js[i]['_then'])
        p2 = get_ast_paths(js[i]['_else'])
        p = [p1[0] + path for path in p2] + p1[1:]
        pv = [cons_calls + [('DBranch', CHILD_EDGE)] + pC[0] + path for path in p]
        p = get_ast_paths(js, i+1)
        ph = [cons_calls + [('DBranch', SIBLING_EDGE)] + path for path in p]
        return ph + pv

    if node_type == 'DExcept':
        p1 = get_ast_paths(js[i]['_try'])
        p2 = get_ast_paths(js[i]['_catch'])
        p = [p1[0] + path for path in p2] + p1[1:]
        pv = [cons_calls + [('DExcept', CHILD_EDGE)] + path for path in p]
        p = get_ast_paths(js, i+1)
        ph = [cons_calls + [('DExcept', SIBLING_EDGE)] + path for path in p]
        return ph + pv

    if node_type == 'DLoop':
        pC = get_ast_paths(js[i]['_cond']) # will have at most 1 "path"
        assert len(pC) <= 1
        p = get_ast_paths(js[i]['_body'])
        pv = [cons_calls + [('DLoop', CHILD_EDGE)] + pC[0] + path for path in p]
        p = get_ast_paths(js, i+1)
        ph = [cons_calls + [('DLoop', SIBLING_EDGE)] + path for path in p]
        return ph + pv

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
        if 'ast' not in program:
            continue
        seqs = get_seqs(program['sequences'])
        ast_paths = get_ast_paths(program['ast']['_nodes'])
        for path in ast_paths:
            path.insert(0, ('DSubTree', CHILD_EDGE))
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
