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

import argparse

from bayou.lda.model import LDA


def vocab(clargs):
    model = LDA(from_file=clargs.input_file[0])
    vocabulary = model.vectorizer.vocabulary_

    if clargs.output_file is not None:
        with open(clargs.output_file, 'w') as f:
            for word in vocabulary:
                f.write(word + '\n')
    else:
        for word in vocabulary:
            print(word)


if __name__ == '__main__':
    argparser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    argparser.add_argument('input_file', type=str, nargs=1,
                           help='input model.pkl file')
    argparser.add_argument('--output_file', type=str, default=None,
                           help='output file to save vocab in')
    clargs = argparser.parse_args()
    vocab(clargs)
