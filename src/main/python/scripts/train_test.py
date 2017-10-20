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

# Use this (interactive) script to split a data file into training and testing data.

import json
import random
import argparse


class message:
    def __init__(self, s):
        self.str = s

    def __enter__(self):
        print(self.str + '...', end='', flush=True)

    def __exit__(self, exc_type, exc_val, exc_tb):
        print('done')


def split(clargs):
    with message('Loading data. This might take a while'), open(clargs.input_file[0]) as f:
        js = json.load(f)

    programs = js['programs']
    total = len(programs)
    print('There are {} programs in total'.format(total))

    randomize = input('Randomize them (y/n)? ')
    if randomize == 'y':
        with message('Randomizing'):
            random.shuffle(programs)

    n = int(input('How many programs in training data? '))
    m = int(input('How many programs in validation data (rest will be in testing)? '))
    training = programs[:n]
    validation = programs[n:n+m]
    testing = programs[n+m:]

    with message('Dumping training data into DATA-training.json'), open('DATA-training.json', 'w') as f:
        json.dump({'programs': training}, fp=f, indent=2)

    with message('Dumping training data into DATA-validation.json'), open('DATA-validation.json', 'w') as f:
        json.dump({'programs': validation}, fp=f, indent=2)

    with message('Dumping testing data into DATA-testing.json'), open('DATA-testing.json', 'w') as f:
        json.dump({'programs': testing}, fp=f, indent=2)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input JSON file')
    clargs = parser.parse_args()
    split(clargs)

