import ijson.backends.yajl2_cffi as ijson
import sys
import json
import argparse

# this script takes in a test bayou dataset and a training bayou dataset
# and return a unique test set that has no duplication in the training

# with open(clargs.input_file[0], 'rb') as f:
#     psis = []
#     labels = []
#     item_num = 0
#     for program in ijson.items(f, 'programs.item'):

# print(json.dumps(progs[0], indent=2, sort_keys=True))

def deduplication(clargs):
    # test_file = clargs.test_file
    # training_file = clargs.training_file
    # output_file = clargs.output_file

    test_set = set()
    with open(clargs.test_file) as f:
        test_progs = json.load(f)['programs']
    for test_prog in test_progs:
        test_set.add(json.dumps(test_prog['ast'], indent=2, sort_keys=True))

    with open(clargs.training_file, 'rb') as f:
        count = 0
        test_set_size = len(test_set)
        print('initial test set size is %d' % test_set_size)
        for train_prog in ijson.items(f, 'programs.item'):
            count += 1
            if count % 1000 == 0:
                print(count)
            single_set = {json.dumps(train_prog['ast'], indent=2, sort_keys=True)}
            test_set.difference_update(single_set)
            if len(test_set) < test_set_size:
                test_set_size -= 1
                print('test set size is reduced to %d' % test_set_size)

    print('done reading the training file!!!')
    print('the size of final unique test set is %d' % test_set_size)

    programs = []
    for test_prog_str in test_set:
        programs.append(json.loads(test_prog_str))
    with open(clargs.output_file, 'w') as f:
        json.dump({'programs': programs}, f, indent=2)
    print('done writing!!!')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--test_file', required=True, type=str,
                        help='the file to remove duplications')
    parser.add_argument('--training_file', required=True, type=str,
                        help='the file to find duplications from')
    parser.add_argument('--output_file', required=True, type=str,
                        help='the file to write unique test file to')
    clargs = parser.parse_args()
    sys.setrecursionlimit(10000)
    deduplication(clargs)