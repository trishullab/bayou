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

import json
import argparse
import subprocess


def javac_and_merge(clargs):
    with open(clargs.input_file[0]) as f:
        js = json.load(f)
    programs_aml = js['programs']
    with open(clargs.data_file) as f:
        js = json.load(f)
    programs = js['programs']
    m, n = len(programs), len(programs_aml)
    assert m == n, 'Data sizes do not match: {} and {}'.format(m, n)

    for i, (program, program_aml) in enumerate(zip(programs, programs_aml)):
        if program_aml == 'ERROR' or compile_javac(program_aml, clargs.classpath):
            program['aml'] = program_aml
            print('done {}/{}'.format(i, m))
        else:
            program['aml'] = 'ERROR'
            print('ERROR {}/{}'.format(i, m))

    print('Writing to {}...'.format(clargs.output_file))
    with open(clargs.output_file, 'w') as f:
        json.dump({'programs': programs}, fp=f, indent=2)
    print('done')


def compile_javac(aml, classpath=None):
    with open('Test.java', 'w') as f:
        f.write(aml)
    try:
        if classpath is None:
            subprocess.check_call(['javac', 'Test.java'], timeout=30)
        else:
            subprocess.check_call(['javac', '-cp', classpath, 'Test.java'], timeout=30)
        return True
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
        return False


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('input_file', type=str, nargs=1,
                        help='output file from the sketch to AML compiler')
    parser.add_argument('--data_file', type=str, required=True,
                        help='main DATA.json file to merge the AML programs with')
    parser.add_argument('--output_file', type=str, required=True,
                        help='file to output merged data')
    parser.add_argument('--classpath', type=str, default=None,
                        help='classpath for the javac process')
    clargs = parser.parse_args()
    javac_and_merge(clargs)
