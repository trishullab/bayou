import argparse
import json

# Use this to extract the top-k distant programs from the testing data set,
# match them with the programs in one of the variational-* or encdec-* files
# (because those files do not have distance info) and generate a new (sub)set
# of variational-* and encdec-* files.

def extract_topk(args):
    with open(args.testing_with_dists[0]) as f:
        js = json.load(f)
    programs = sorted(js['programs'], key=lambda p: -p['corpus_dist'])

    # for each program, check if there's a matching one in the provided JSON
    # (there HAS to be one if the program was tested on)
    with open(args.predict_asts_output) as f:
        testing_corpus = json.load(f)
    matches = []
    found = 0
    for i, program in enumerate(programs):
        print('Matches found for {}/{} programs'.format(found, i), end='\r')
        matched_program = match(program, testing_corpus)
        if matched_program is not None:
            matches.append(matched_program)
            found += 1
    print('Matches found for {}/{} programs'.format(found, i))

    # do the top-k
    output_programs = sorted(matches, key=lambda p: -p['corpus_dist'])[:args.k]

    # dump to file
    with open(args.output_file, 'w') as f:
        json.dump({ 'programs': output_programs }, f, indent=2)

def match(program, testing_corpus):
    for testing_program in testing_corpus['programs']:
        if testing_program['original_ast'] == program['ast']:
            testing_program['corpus_dist'] = program['corpus_dist']
            return testing_program
    return None

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('testing_with_dists', type=str, nargs=1,
                       help='input file, the testing data with distances')
    parser.add_argument('--predict_asts_output', type=str, required=True,
                       help='data file containing output from predict_asts (variational-*, encdec-*.json)')
    parser.add_argument('--output_file', type=str, required=True,
                       help='output file to print to')
    parser.add_argument('--k', type=int, required=True,
                        help='the top-K (NOTE: the actual number, not top-K%). Find this using other means.')
    args = parser.parse_args()
    extract_topk(args)
