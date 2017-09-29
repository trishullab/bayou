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

# Use this to remove all generics from a DATA.json file.
# Usage: remove_generics.py <DATA.json> <DATA-output.json>

import json
import sys


def remove_generics_call(call):
    in_generic = 0
    call_no_generics = ''
    for c in call:
        if c == '<':
            in_generic += 1
        if in_generic == 0:
            call_no_generics += c
        if c == '>':
            in_generic -= 1
    return call_no_generics


def remove_generics_dsubtree(n):
    for node in n['_nodes']:
        remove_generics(node)


def remove_generics_dapicall(n):
    n['_call'] = remove_generics_call(n['_call'])


def remove_generics_dbranch(n):
    for c in n['_cond']:
        remove_generics_dapicall(c)
    for node in n['_then']:
        remove_generics(node)
    for node in n['_else']:
        remove_generics(node)


def remove_generics_dloop(n):
    for c in n['_cond']:
        remove_generics_dapicall(c)
    for node in n['_body']:
        remove_generics(node)


def remove_generics_dexcept(n):
    for node in n['_try']:
        remove_generics(node)
    for node in n['_catch']:
        remove_generics(node)


def remove_generics(n):
    if n['node'] == 'DSubTree':
        remove_generics_dsubtree(n)
    elif n['node'] == 'DAPICall':
        remove_generics_dapicall(n)
    elif n['node'] == 'DBranch':
        remove_generics_dbranch(n)
    elif n['node'] == 'DLoop':
        remove_generics_dloop(n)
    elif n['node'] == 'DExcept':
        remove_generics_dexcept(n)
    else:
        raise TypeError('Invalid node type')


def remove_generics_sequences(seqs):
    for seq in seqs:
        for j, call in enumerate(seq['calls']):
            seq['calls'][j] = remove_generics_call(call)


if not len(sys.argv) == 3:
    print('Usage: remove_generics.py <DATA.json> <DATA-output.json>')
    sys.exit(1)

print('Reading...')
with open(sys.argv[1]) as f:
    js = json.load(f)

print('Removing generics...')
l = len(js['programs'])
for i, p in enumerate(js['programs']):
    ast = p['ast']
    remove_generics(ast)
    remove_generics_sequences(p['sequences'])
    print('{:8d}/{} programs done'.format(i, l), end='\r')
print('{:8d}/{} programs done'.format(i, l))

print('Writing...')
with open(sys.argv[2], 'w') as f:
    json.dump(js, fp=f, indent=2)
