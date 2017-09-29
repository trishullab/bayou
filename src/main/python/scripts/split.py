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
