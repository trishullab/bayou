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
    print('total number of useful programs is %' % len(probs))
    probs = sorted(probs, reverse=True)
    import pdb; pdb.set_trace()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, help='input data file')
    parser.add_argument('--out', type=str, default='temp.out')
    clargs = parser.parse_args()
    compute_stats(clargs)
