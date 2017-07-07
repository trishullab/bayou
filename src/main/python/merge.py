from __future__ import print_function

# Use this script to merge data files in a folder (the opposite of split.py).
# The script will accept a folder containing all the JSON files, and it will
# merge them into a given file.

import os
import json
import argparse


def merge(clargs):
    programs = []
    for filename in sorted(os.listdir(clargs.folder[0])):
        with open(os.path.join(clargs.folder[0], filename)) as f:
            js = json.load(f)
        programs += (js['programs'])
    with open(clargs.output_file, 'w') as f:
        json.dump({'programs': programs}, f, indent=2)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('folder', type=str, nargs=1,
                        help='folder where all JSON files are stored')
    parser.add_argument('--output_file', type=str, required=True,
                        help='file to output merged data')
    clargs = parser.parse_args()
    merge(clargs)
