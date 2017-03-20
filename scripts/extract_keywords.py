from __future__ import print_function
import json
import sys
import itertools
import re

def split_camel(s):
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1#\2', s) # LC followed by UC
    s1 = re.sub('([a-z0-9])([A-Z])', r'\1#\2', s1) # UC followed by LC
    return s1.split('#')

def get_name(call):
    q = call.split('(')[0].split('.')
    cls, name = q[-2], q[-1]
    return cls + '#' + name

if not len(sys.argv) == 3:
    print('Usage: extract_keywords.py <DATA-input.json> <DATA-output.json>')
    sys.exit(1)

with open(sys.argv[1]) as f:
    js = json.load(f)

for program in js['programs']:
    calls = set([get_name(call) for sequence in program['sequences'] for call in sequence['calls']])
    keywords = list(itertools.chain.from_iterable([split_camel(call) for call in calls]))
    program['keywords'] = list(set([kw.lower() for kw in keywords if not kw == '']))

with open(sys.argv[2], 'w') as f:
    json.dump(js, f, indent=2)
