from __future__ import print_function

# Use this script to split data between N processes. The script will accept a
# JSON file containing the data (such as DATA-testing.json, variational-*.json,
# etc.) and split it into N files

import json
import math
import argparse


def split(args):
    with open(args.input_file[0]) as f:
        js = json.load(f)
    programs = js['programs']
    n = int(math.ceil(float(len(programs)) / args.splits))
    split_programs = [programs[i*n:i*n+n] for i in range(args.splits)]
    for i, programs in enumerate(split_programs):
        with open('{}-{}.json'.format(args.input_file[0][:-5], i), 'w') as f:
            json.dump({'programs': programs}, f, indent=2)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input JSON file')
    parser.add_argument('--splits', type=int, required=True,
                        help='number of splits')
    args = parser.parse_args()
    split(args)
