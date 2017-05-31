from __future__ import print_function
import sys
import zss
import re
import json
import argparse
import editdistance

# WARNING: This experiment might take a long time. Make sure the data is split
# and the execution of this script is parallelized. Use split.py (and later
# merge.py) for this purpose.

def editdist(args):
    with open(args.input_file[0]) as f:
        js = json.load(f)
    with open(args.corpus) as f:
        corpus = json.load(f)
    for i, program in enumerate(js['programs']):
        program['corpus_dist'] = int(closest_dist(program['ast'], corpus))
        print('Done with {} programs'.format(i))
    with open(args.output_file, 'w') as f:
        json.dump(js, f, indent=2)

def closest_dist(ast, corpus):
    dists = [zss.simple_distance(ast, program['ast'],
                get_children=ZSS.get_children,
                get_label=ZSS.get_label,
                label_dist=ZSS.label_dist_string) \
                    for program in corpus['programs']]
    return min(dists)

class ZSS(object):
    @staticmethod
    def get_label(js):
        return js['_call'] if js['node'] == 'DAPICall' else js['node']

    @staticmethod
    def get_children(js):
        node = js['node']
        if node == 'DAPICall':
            return []
        elif node == 'DBranch':
            return js['_cond'] + js['_then'] + js['_else']
        elif node == 'DExcept':
            return js['_try'] + js['_catch']
        elif node == 'DLoop':
            return js['_cond'] + js['_body']
        elif node == 'DSubTree':
            return js['_nodes']
        else:
            raise TypeError('Invalid tree node: '.format(node))

    @staticmethod
    def label_dist_string(label1, label2):
        return editdistance.eval(label1, label2)

    @staticmethod
    def label_dist(label1, label2):
        def get_method(call):
            return call.split('(')[0]
        def get_class(call):
            method = get_method(call)
            return method[:method.rfind('.')]
        def get_package(call):
            cls = get_class(call)
            return re.compile('\.[A-Z]').split(cls)[0]

        if label1 == label2:
            return 0
        if get_method(label1) == get_method(label2):
            return 1
        if get_class(label1) == get_class(label2):
            return 2
        if get_package(label1) == get_package(label2):
            return 4
        return 100

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('input_file', type=str, nargs=1,
                       help='input file, the testing data')
    parser.add_argument('--corpus', type=str, required=True,
                       help='the training data file')
    parser.add_argument('--output_file', type=str, required=True,
                       help='output file to print to')
    args = parser.parse_args()
    editdist(args)
