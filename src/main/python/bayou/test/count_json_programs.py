import json
import sys

file_name = sys.argv[1]
print('read file: %s' % file_name)
sys.setrecursionlimit(10000)

with open(file_name) as f:
    progs = json.load(f)['programs']

size = len(progs)
print('the number of programs in this json file is %d' % size)
