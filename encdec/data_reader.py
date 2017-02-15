import os
import sys
import json
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

def select_samples(powerset, args):
    def is_contained(seq1, seq2): # True if seq1 is a sub-sequence (starting from index 0) of seq2
        if len(seq1) > len(seq2):
            return False
        for i, call in enumerate(seq1):
            if not seq2[i] == call:
                return False
        return True
    def set_is_ok(_s):
        s = list(_s)
        if len(s) > args.max_seqs:
            return False
        for i, seq in enumerate(s):
            if len(seq) > args.max_seq_length:
                return False
            for j, seq2 in enumerate(s):
                if not i == j and is_contained(seq, seq2):
                    return False
        return True
    samples = filter(set_is_ok, powerset)
    return list(samples)

def blowup_and_sample(seqs, args):
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
    samples = select_samples(powerset, args)
    return samples

def read_data(filename, args):
    with open(filename) as f:
        js = json.loads(f.read())
    inputs, targets = [], []
    ignored, done = 0, 0
    count_sets_of_seqs = 0

    for program in js['programs']:
        if 'ast' not in program:
            continue
        seqs = get_seqs(program['sequences'])
        ast_paths = get_ast_paths(program['ast']['_nodes'])
        for path in ast_paths:
            path.insert(0, ('DSubTree', CHILD_EDGE))
        try:
            assert all([len(path) <= args.max_ast_depth for path in ast_paths])
            samples = blowup_and_sample(seqs, args)
            count_sets_of_seqs += len(samples)
            for subset in samples:
                for path in ast_paths:
                    inputs.append(sorted(subset))
                    targets.append(path)
        except AssertionError:
            ignored += 1
        done += 1
        print('{:8d} programs done'.format(done), end='\r')

    print('\n{:8d} programs ignored'.format(ignored))
    print('{:8d} (seqs, AST) pairs total'.format(count_sets_of_seqs))
    return inputs, targets

if __name__ == '__main__':
    print_data(sys.argv[1])
