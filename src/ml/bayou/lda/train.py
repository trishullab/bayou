import os
import sys
import json
import pickle
import argparse

from bayou.lda.model import LDA


def train(clargs):
    print('Reading data file...')
    data = get_data(clargs.input_file[0], clargs.evidence)

    ok = 'r'
    while ok == 'r':
        model = LDA(args=clargs)
        model.train(data)
        top_words = model.top_words(clargs.top)
        for i, words in enumerate(top_words):
            print('\nTop words in Topic#{:d}'.format(i))
            for w in words:
                print('{:.2f} {:s}'.format(words[w], w))
        print('\nOK with the model (y(es)/n(o)/r(edo))? ', end='')
        ok = sys.stdin.readline().rstrip('\n')

    if ok == 'y':
        print('Saving model to {:s}'.format(os.path.join(clargs.save, 'model.pkl')))
        with open(os.path.join(clargs.save, 'model.pkl'), 'wb') as fmodel:
            pickle.dump((model.model, model.vectorizer), fmodel)


def get_data(input_file, evidence):
    with open(input_file) as f:
        js = json.loads(f.read())
    data = []
    nprograms = len(js['programs'])

    for i, program in enumerate(js['programs']):
        bow = set(program[evidence])
        data.append(bow)
        print('Gathering data for LDA: {:5d}/{:d} programs'.format(i+1, nprograms), end='\r')
    print()
    return data


if __name__ == '__main__':
    argparser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    argparser.add_argument('input_file', type=str, nargs=1,
                           help='input JSON file')
    argparser.add_argument('--ntopics', type=int, required=True,
                           help='run LDA with n topics')
    argparser.add_argument('--evidence', choices=['keywords', 'types'], required=True,
                           help='the type of evidence for which LDA is run')
    argparser.add_argument('--save', type=str, default='save',
                           help='directory to store LDA model')
    argparser.add_argument('--alpha', type=float, default=0.1,
                           help='initial alpha value')
    argparser.add_argument('--top', type=int, default=5,
                           help='top-k words to print from each topic')
    clargs = argparser.parse_args()
    train(clargs)
