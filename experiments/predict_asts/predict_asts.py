import tensorflow as tf
import argparse
import json
import time
import random

from encdec.predict_ast import Predictor
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
    parser.add_argument('--infer_seqs', type=str, choices=['all', 'half', 'one'], default='all',
                       help='number of sequences to provide for inference')
    parser.add_argument('--seed', type=int, default=1234,
                       help='seed to use for random')
    parser.add_argument('--output_file', type=str, default=None,
                       help='output file to print predicted ASTs')
    args = parser.parse_args()
    random.seed(args.seed)
    print(args)

    with tf.Session() as sess:
        if args.model == 'encdec':
            predictor = Predictor(args.save_dir, sess)
        else:
            predictor = VariationalPredictor(args.save_dir, sess)

        data = gather_data(args.input_file[0], predictor.model.args, args.infer_seqs)
        programs = []
        for j, (given_seqs, unseen_seqs, org_ast) in enumerate(data):
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
            js_dict = { 'original_ast': org_ast,
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
    given_seqs, unseen_seqs, asts = [], [], []
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
        if infer_seqs == 'all': # shuffle if all
            random.shuffle(sub_seqs)
            seqs_to_append = sub_seqs
        if infer_seqs == 'half':
            seqs_to_append = sub_seqs[int(len(sub_seqs)/2):]
        if infer_seqs == 'one':
            seqs_to_append = [sub_seqs[-1]]
        given_seqs.append(seqs_to_append)
        unseen_seqs.append([seq for seq in sub_seqs if seq not in seqs_to_append])
        asts.append(program['ast'])
    return zip(given_seqs, unseen_seqs, asts)

if __name__ == '__main__':
    main()
