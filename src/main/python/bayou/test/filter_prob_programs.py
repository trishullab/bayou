import argparse
import json


def compute_stats(clargs):
    input_file = clargs.input_file
    with open(input_file) as f:
        progs = json.load(f)['programs']
    probs = []
    for prog in progs:
        ast = prog['ast']
        if 'cond_prob' in ast:
            probs.append(ast['cond_prob'])
    print('total number of useful programs is %i' % len(probs))
    probs = sorted(probs, reverse=True)
    import pdb; pdb.set_trace()


def filter_progs(clargs, reserve_num=400000):
    input_file = clargs.input_file
    with open(input_file) as f:
        progs = json.load(f)['programs']
    progs_probs = {}
    for prog in progs:
        ast = prog['ast']
        if 'cond_prob' in ast:
            progs_probs[prog] = ast['cond_prob']
    print('total number of useful programs is %i' % len(progs_probs))
    progs_probs_items = sorted(progs_probs, key=lambda kv: kv[1], reverse=True)
    reserved_progs = [prog for (prog, prob) in progs_probs_items[:reserve_num]]
    print('number of reserved programs is %i' % len(reserved_progs))
    import pdb; pdb.set_trace()
    with open(clargs.out, 'w') as f:
        json.dump({'programs': reserved_progs}, f, indent=2)
    print('done')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, help='input data file')
    parser.add_argument('--out', type=str, default='temp.out')
    clargs = parser.parse_args()
    # compute_stats(clargs)
    filter_progs(clargs)