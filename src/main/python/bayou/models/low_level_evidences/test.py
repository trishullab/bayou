# Copyright 2017 Rice University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import print_function
import numpy as np
import tensorflow as tf

import argparse
import os
import sys
import simplejson as json
import textwrap

import time
#import bayou.models.core.infer
import bayou.models.low_level_evidences.infer
from bayou.models.low_level_evidences.utils import read_config, normalize_log_probs, find_my_rank, rank_statistic, ListToFormattedString
from bayou.models.low_level_evidences.data_reader import Reader


File_Name = 'Search_Data_Basic'

HELP = """ Help me! :( """
#%%

def test(clargs):
    a1s, b1s, a2s, b2s, prob_Ys  = test_get_vals(clargs)


    latent_size, num_progs, batch_size = len(b1s[0]), len(a1s), 10000
    hit_points = [1,2,5,10,50,100,500,1000,5000,10000]
    hit_counts = np.zeros(len(hit_points))
    for i in range(num_progs):
        prob_Y_Xs = []
        for j in range(int(np.ceil(num_progs / batch_size))):
            sid, eid = j * batch_size, min( (j+1) * batch_size , num_progs)
            prob_Y_X = get_c_minus_cstar(np.array(a1s[i]), np.array(b1s[i]),\
                                    np.array(a2s[sid:eid]), np.array(b2s[sid:eid]), np.array(prob_Ys[sid:eid]), latent_size)
            prob_Y_Xs += list(prob_Y_X)

        _rank = find_my_rank( prob_Y_Xs , i )

        hit_counts, prctg = rank_statistic(_rank, i + 1, hit_counts, hit_points)

        if (((i+1) % 100 == 0) or (i == (num_progs - 1))):
            print('Searched {}/{} (Max Rank {})'
                  'Hit_Points {} :: Percentage Hits {}'.format
                  (i + 1, num_progs, num_progs,
                   ListToFormattedString(hit_points, Type='int'), ListToFormattedString(prctg, Type='float')))
    return


def test_get_vals(clargs):

    infer_vars, config = forward_pass(clargs)
    import gc
    gc.collect()

    programs = []
    a1s,a2s,b1s,b2s,prob_Ys  = [],[],[],[],[]
    i=0
    j=0
    for prog_id in sorted(list(infer_vars.keys())):

        if True:
           #a1s += [infer_vars[prog_id]['a1']]
           a2s += [infer_vars[prog_id]['a2']]
           #b1s += [list(infer_vars[prog_id]['b1'])]
           b2s += [list(infer_vars[prog_id]['b2'])]
           prob_Ys += [infer_vars[prog_id]['ProbY']]
           #Ys += [infer_vars[prog_id]['Y']]

           program = infer_vars[prog_id]['JS']
           #if program['returnType']=='None':
           #   program['returnType'] = 'void'
           # a1, a2 and ProbY are all scalars, b1 and b2 are vectors
           #program['a1'] = infer_vars[prog_id]['a1'].item()
           #program['b1'] = [val.item() for val in infer_vars[prog_id]['b1']]
           program['a2'] = "%.3f" % infer_vars[prog_id]['a2'].item()
           program['b2'] = [ "%.3f" % val.item() for val in infer_vars[prog_id]['b2']]
           program['ProbY'] = "%.3f" % infer_vars[prog_id]['ProbY'].item()

           del infer_vars[prog_id]

           programs.append(program)

           #prob_Ys = normalize_log_probs(prob_Ys)
           #print('Normalizing done')
           i += 1

           if i % 100000 == 0:
               j += 1

               fileName = "Program_output_" + str(j) + ".json"
               print('\nWriting to {}...'.format(fileName), end='')
               with open(fileName, 'w') as f:
                    json.dump({'programs': programs}, fp=f, indent=2)
               programs = []
               a1s,a2s,b1s,b2s,prob_Ys  = [],[],[],[],[]


    print('New arrays saving done')
    return a1s, b1s, a2s, b2s, prob_Ys

def forward_pass(clargs):
    #set clargs.continue_from = True while testing, it continues from old saved config
    clargs.continue_from = True

    with open(os.path.join(clargs.save, 'config.json')) as f:
        model_type = json.load(f)['model']

    if model_type == 'lle':
        model = bayou.models.low_level_evidences.infer.BayesianPredictor
    else:
        raise ValueError('Invalid model type in config: ' + model_type)

    # load the saved config
    with open(os.path.join(clargs.save, 'config.json')) as f:
        config = read_config(json.load(f), chars_vocab=True)
    config.batch_size = 500

    reader = Reader(clargs, config, infer=True)

	# Placeholders for tf data
    prog_ids_placeholder = tf.placeholder(reader.prog_ids.dtype, reader.prog_ids.shape)
    js_prog_ids_placeholder = tf.placeholder(reader.js_prog_ids.dtype, reader.js_prog_ids.shape)
    nodes_placeholder = tf.placeholder(reader.nodes.dtype, reader.nodes.shape)
    edges_placeholder = tf.placeholder(reader.edges.dtype, reader.edges.shape)
    targets_placeholder = tf.placeholder(reader.targets.dtype, reader.targets.shape)
    evidence_placeholder = [tf.placeholder(input.dtype, input.shape) for input in reader.inputs]

    # reset batches

    feed_dict={fp: f for fp, f in zip(evidence_placeholder, reader.inputs)}
    feed_dict.update({prog_ids_placeholder: reader.prog_ids})
    feed_dict.update({js_prog_ids_placeholder: reader.js_prog_ids})
    feed_dict.update({nodes_placeholder: reader.nodes})
    feed_dict.update({edges_placeholder: reader.edges})
    feed_dict.update({targets_placeholder: reader.targets})

    dataset = tf.data.Dataset.from_tensor_slices((prog_ids_placeholder, js_prog_ids_placeholder, nodes_placeholder, edges_placeholder, targets_placeholder, *evidence_placeholder))
    batched_dataset = dataset.batch(config.batch_size)
    iterator = batched_dataset.make_initializable_iterator()
    jsp = reader.js_programs
    with tf.Session() as sess:
        predictor = model(clargs.save, sess, config, iterator, bayou_mode = False) # goes to infer.BayesianPredictor
        # testing
        sess.run(iterator.initializer, feed_dict=feed_dict)
        infer_vars = {}


        allEvSigmas = predictor.get_ev_sigma()
        print(allEvSigmas)

        for j in range(config.num_batches):
            prob_Y, a1, b1, a2, b2, js_prog_ids, prog_ids = predictor.get_all_params_inago()
            for i in range(config.batch_size):
                prog_id = prog_ids[i]
                if prog_id not in infer_vars:
                    infer_vars[prog_id] = {}
                    #infer_vars[prog_id]['a1'] = a1[i].round(decimals=2)
                    infer_vars[prog_id]['a2'] = a2[i].round(decimals=2)
                    #infer_vars[prog_id]['b1'] = b1[i].round(decimals=2)
                    infer_vars[prog_id]['b2'] = b2[i].round(decimals=2)
                    infer_vars[prog_id]['ProbY'] = prob_Y[i].round(decimals=2)
                    infer_vars[prog_id]['count_prog_ids'] = 1
                    infer_vars[prog_id]['JS'] = jsp[js_prog_ids[i]]
                else:
                    infer_vars[prog_id]['a2'] += a2[i].round(decimals=2)
                    infer_vars[prog_id]['b2'] += b2[i].round(decimals=2)
                    infer_vars[prog_id]['ProbY'] = np.logaddexp( infer_vars[prog_id]['ProbY'] , prob_Y[i].round(decimals=2) )
                    infer_vars[prog_id]['count_prog_ids'] += 1


            if (j+1) % 1000 == 0:
                print('Completed Processing {}/{} batches'.format(j+1, config.num_batches))



    print('Batch Processing Completed')

    for prog_id in list(infer_vars.keys()):
        infer_vars[prog_id]['a2'] /= infer_vars[prog_id]['count_prog_ids']
        infer_vars[prog_id]['a2'] = infer_vars[prog_id]['a2'].round(decimals=2)

        infer_vars[prog_id]['b2'] /= infer_vars[prog_id]['count_prog_ids']
        infer_vars[prog_id]['b2'] = infer_vars[prog_id]['b2'].round(decimals=2)

        infer_vars[prog_id]['ProbY'] -= np.log(infer_vars[prog_id]['count_prog_ids']) # prob_Ys are added and it should not be averaged, well technically
        infer_vars[prog_id]['ProbY'] = infer_vars[prog_id]['ProbY'].round(decimals=2)

    print('Program Average done')

    del predictor, iterator, batched_dataset, dataset, jsp, feed_dict, reader, prog_ids_placeholder, js_prog_ids_placeholder, nodes_placeholder, edges_placeholder, targets_placeholder, evidence_placeholder

    return infer_vars, config


def get_c_minus_cstar(a1, b1, a2, b2, prob_Y, latent_size):
    # all inputs are np.arrays
    a_star = a1 + a2 + 0.5 # shape is [batch_size]
    b_star = np.expand_dims(b1,axis=0) + b2  # shape is [batch_size, latent_size]

    ab1 = np.sum(np.square(b1)/(4*a1), axis=0) + 0.5 * latent_size * np.log(-a1/np.pi) # shape is ()
    ab2 = np.sum(np.square(b2)/(4*np.tile(np.expand_dims(a2,1), [1,latent_size])), axis=1) \
                        + 0.5 *  latent_size * np.log(-a2/np.pi) # shape is [batch_size]
    ab_star = np.sum(np.square(b_star)/(4* np.tile(np.expand_dims(a_star,1), [1,latent_size])), axis=1) \
                        + 0.5 *  latent_size * np.log(-a_star/np.pi) # shape is [batch_size]
    cons = 0.5 * latent_size * np.log( 2*np.pi )

    prob = ab1 + ab2 - ab_star - cons
    prob += prob_Y
    return prob

#%%
if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter,
                                     description=textwrap.dedent(HELP))
    parser.add_argument('--python_recursion_limit', type=int, default=10000,
                        help='set recursion limit for the Python interpreter')
    parser.add_argument('--save', type=str, required=True,
                        help='checkpoint model during training here')
    parser.add_argument('--evidence', type=str, default='all',
                        choices=['apicalls', 'types', 'keywords', 'all'],
                        help='use only this evidence for inference queries')
    parser.add_argument('--output_file', type=str, default=None,
                        help='output file to print probabilities')

    #clargs = parser.parse_args()
    clargs = parser.parse_args(['--save', 'save1'])

    sys.setrecursionlimit(clargs.python_recursion_limit)
    test(clargs)
