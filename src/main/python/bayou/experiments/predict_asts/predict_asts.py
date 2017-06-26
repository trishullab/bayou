import tensorflow as tf
import argparse
import json
import time

import bayou.core.infer
import bayou.experiments.nonbayesian.infer
import bayou.experiments.low_level_evidences.infer

TIMEOUT = 20  # seconds per query


def main(clargs):
    with open(clargs.input_file[0]) as f:
        js = json.load(f)
    programs = js['programs']

    with tf.Session() as sess:
        if clargs.model == 'bayesian':
            p_type = bayou.core.infer.BayesianPredictor
        elif clargs.model == 'nonbayesian':
            p_type = bayou.experiments.nonbayesian.infer.NonBayesianPredictor
        elif clargs.model == 'low_level_evidences':
            p_type = bayou.experiments.low_level_evidences.infer.BayesianPredictor
        else:
            raise TypeError('invalid type of model specified')
        print('Loading model...')
        predictor = p_type(clargs.save, sess)

        for i, program in enumerate(programs):
            start = time.time()
            if not clargs.evidence == 'all':
                if program[clargs.evidence] is []:
                    program['out_asts'] = []
                    print('Program {}, {} ASTs, {:.2f}s'.format(
                        i, len(program['out_asts']), time.time() - start))
                    continue
                evidences = {clargs.evidence: program[clargs.evidence]}
            else:
                evidences = program
            asts, counts = [], []
            for j in range(100):
                if time.time() - start > TIMEOUT:
                    break
                try:
                    ast = predictor.infer(evidences)
                except AssertionError:
                    continue
                try:
                    counts[asts.index(ast)] += 1
                except ValueError:
                    asts.append(ast)
                    counts.append(1)
            for ast, count in zip(asts, counts):
                ast['count'] = count
            asts.sort(key=lambda x: x['count'], reverse=True)
            program['out_asts'] = asts[:10]
            print('Program {}, {} ASTs, {:.2f}s'.format(
                i, len(program['out_asts']), time.time() - start))

        if clargs.output_file is None:
            print(json.dumps({'programs': programs}, indent=2))
        else:
            with open(clargs.output_file, 'w') as f:
                json.dump({'programs': programs}, f, indent=2)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--save', type=str, required=True,
                        help='directory to load model from')
    parser.add_argument('--model', type=str, required=True,
                        choices=['bayesian', 'nonbayesian', 'low_level_evidences'],
                        help='the type of model')
    parser.add_argument('--evidence', type=str, default='all',
                        choices=['apicalls', 'types', 'context', 'all'],
                        help='use only this evidence for inference queries')
    parser.add_argument('--output_file', type=str, default=None,
                        help='output file to print predicted ASTs')
    clargs = parser.parse_args()
    print(clargs)
    main(clargs)
