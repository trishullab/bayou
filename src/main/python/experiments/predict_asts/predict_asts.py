import tensorflow as tf
import argparse
import json
import time
import math
import random

from experimental.encdec.predict_ast import Predictor
from variational.predict_ast import VariationalPredictor
from variational.data_reader import get_seqs, sub_sequences

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('input_file', type=str, nargs=1,
                       help='input data file')
    parser.add_argument('--save_dir', type=str, required=True,
                       help='directory where model is saved')
    parser.add_argument('--model', type=str, required=True, choices=['encdec', 'variational'],
                       help='the type of model')
    parser.add_argument('--n', type=int, default=1,
                       help='number of ASTs to generate for each set of sequences')
    parser.add_argument('--infer_seqs', type=float, default=1.,
                       help='percentage of sequences (range (0,1]) to provide for inference')
    parser.add_argument('--seed', type=int, default=1234,
                       help='seed to use for random')
    parser.add_argument('--output_file', type=str, default=None,
                       help='output file to print predicted ASTs')
    args = parser.parse_args()
    if args.infer_seqs <= 0. or args.infer_seqs > 1.:
        parser.error('infer_seqs must be in range (0,1]')
    random.seed(args.seed)
    print(args)

    with tf.Session() as sess:
        if args.model == 'encdec':
            predictor = Predictor(args.save_dir, sess)
        else:
            predictor = VariationalPredictor(args.save_dir, sess)

        data = gather_data(args.input_file[0], predictor.model.args, args.infer_seqs)
        programs = []
        for j, (filename, given_seqs, unseen_seqs, org_ast) in enumerate(data):
            try:
                predictor_input = given_seqs if args.model == 'encdec' \
                                             else predictor.psi_from_seqs(given_seqs)
            except TypeError:
                continue
            asts = []
            for i in range(args.n):
                try:
                    ast = predictor.generate_ast(predictor_input)[0]
                    asts.append(ast)
                except (AssertionError,  TypeError):
                    continue
            js_dict = { 'file': filename,
                        'original_ast': org_ast,
                        'given_sequences': given_seqs,
                        'unseen_sequences': unseen_seqs,
                        'predicted_asts': asts } 
            programs.append(js_dict)
            print('Done with {} programs'.format(j))

        if args.output_file is None:
            print(json.dumps({ 'programs': programs }, indent=2))
        else:
            with open(args.output_file, 'w') as f:
                json.dump({ 'programs': programs }, f, indent=2)

def gather_data(input_file, saved_args, infer_seqs):
    filenames, given_seqs, unseen_seqs, asts = [], [], [], []
    with open(input_file) as f:
        js = json.load(f)
    for program in js['programs']:
        if 'ast' not in program or 'sequences' not in program:
            continue
        seqs_program = get_seqs(program['sequences'])
        try:
            sub_seqs = sub_sequences(seqs_program, saved_args)
        except AssertionError:
            continue
        selected_seqs = sub_seqs[-math.ceil(len(sub_seqs)*infer_seqs):]
        given_seqs.append(selected_seqs)
        unseen_seqs.append([seq for seq in sub_seqs if seq not in given_seqs[-1]])
        asts.append(program['ast'])
        filenames.append(program['file'])
    return zip(filenames, given_seqs, unseen_seqs, asts)

if __name__ == '__main__':
    main()
