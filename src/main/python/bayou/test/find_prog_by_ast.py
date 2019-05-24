import ijson.backends.yajl2_cffi as ijson
import sys
import json
import argparse

# print(json.dumps(progs[0], indent=2, sort_keys=True))

def find(clargs):
    ast_str_set = set()
    with open(clargs.ast_file) as f:
        asts = json.load(f)['programs']
    for ast in asts:
        ast_str_set.add(json.dumps(ast, indent=2, sort_keys=True))

    programs = []

    with open(clargs.prog_file, 'rb') as f:
        ast_str_set_size = len(ast_str_set)
        print('initial ast string set size is %d' % ast_str_set_size)
        for prog in ijson.items(f, 'programs.item'):
            single_set = {json.dumps(prog['ast'], indent=2, sort_keys=True)}
            ast_str_set.difference_update(single_set)
            if len(ast_str_set) < ast_str_set_size:
                ast_str_set_size -= 1
                print('ast string set size is reduced to %d' % ast_str_set_size)
                programs.append(prog)
                if ast_str_set_size == 0:
                    break
    print('done finding!!!')

    with open(clargs.output_file, 'w') as f:
        json.dump({'programs': programs}, f, indent=2)
    print('done writing!!!')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--ast_file', required=True, type=str)
    parser.add_argument('--prog_file', required=True, type=str)
    parser.add_argument('--output_file', required=True, type=str)
    clargs = parser.parse_args()
    sys.setrecursionlimit(10000)
    find(clargs)