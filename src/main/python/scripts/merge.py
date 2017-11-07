# Copyright 2017 Rice University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import print_function

# Use this script to merge data files in a folder (the opposite of split.py).
# The script will accept a folder containing all the JSON files, and it will
# merge them into a given file.

import sys
import json
import argparse


def merge(clargs):
    programs = []
    with open(clargs.file_list[0]) as f:
        file_list = f.readlines()
    for filename in file_list:
        filename = filename[:-1]  # ignore '\n'
        try:
            with open(filename) as f:
                js = json.load(f)
        except ValueError:
            print('Error merging file: {}'.format(filename))
            continue
        programs += (js['programs'])
    with open(clargs.output_file, 'w') as f:
        json.dump({'programs': programs}, f, indent=2)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('file_list', type=str, nargs=1,
                        help='file containing list of all JSON files')
    parser.add_argument('--python_recursion_limit', type=int, default=10000,
                        help='set recursion limit for the Python interpreter')
    parser.add_argument('--output_file', type=str, required=True,
                        help='file to output merged data')
    clargs = parser.parse_args()
    sys.setrecursionlimit(clargs.python_recursion_limit)
    merge(clargs)
