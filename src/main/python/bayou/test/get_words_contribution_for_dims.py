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

import tensorflow as tf
import argparse
import json
import os
import sys
import numpy as np
import time

import bayou.models.low_level_evidences.infer


def get(clargs):
    with open(clargs.input_file[0]) as f:
        js = json.load(f)
    model = bayou.models.low_level_evidences.infer.BayesianPredictor
    programs = js['programs']

    with tf.Session() as sess:
        print('Loading model...')
        predictor = model(clargs.save, sess, embed_file=clargs.embedding_file)

        with open(os.path.join(clargs.save, 'config.json')) as f:
            config_file = json.load(f)

        # process data into batches
        batch_size = config_file['batch_size']
        print('batch size is {}'.format(batch_size))
        num_batches = int(len(programs) / batch_size)
        print('number of batches is {}'.format(num_batches))
        programs_size = batch_size * num_batches
        print('the number of programs after processing is {}'.format(programs_size))
        programs = programs[:programs_size]
        # only one type of evidence 'Javadoc'
        ev = predictor.model.config.evidence[0]
        data_points = []
        for prog in programs:
            data_points.append(ev.read_data_point(prog))
        # numpy array [sz, max_words+1]
        raw_inputs = ev.wrangle(data_points)
        input_batches = np.split(raw_inputs, num_batches, axis=0)

        # retrieve useful parameters
        rnn_units = config_file['evidence'][0]['rnn_units']
        max_words = config_file['evidence'][0]['max_words']
        print('rnn_units and max_words of "Javadoc" is {} and {}'.format(rnn_units, max_words))
        latent_size = config_file['latent_size']
        print('latent size is {}'.format(latent_size))
        chars = config_file['evidence'][0]['chars']
        vocab = dict(zip(range(len(chars)), chars))

        # format per-program: {'words': '...', 'multi or weights or softmax': list of list, 'res': list}
        outputs = []

        print('total number of batches is %i' % len(input_batches))
        batch_counter = 0

        for batch in input_batches:
            batch_counter += 1
            print("it's the %ith batch now" % batch_counter)
            batch_start = time.time()
            feed = {}
            feed[predictor.model.encoder.inputs[0].name] = batch

            # two groups of tensors
            # 'latent_size' list 'multi_outputs', each element (batch_size, max_words)
            # 'latent_size' list 'latent_dims', each element (batch_size)
            shared_list = sess.run(fetches=ev.multi_outputs+ev.latent_dims, feed_dict=feed)
            local_multi_outputs = shared_list[:latent_size]
            local_latent_dims = shared_list[latent_size:]

            for i in range(batch_size):
                output = {}
                words_length = batch[i][max_words]
                words_indices = batch[i][:words_length]
                words = [vocab[idx] for idx in words_indices]
                output['words'] = ' '.join(words)

                out_multi_outputs = []
                out_latent_dims = []
                output['out_multi_outputs'] = out_multi_outputs
                output['out_latent_dims'] = out_latent_dims

                for j in range(latent_size):
                    out_multi_outputs.append(local_multi_outputs[j][i][:words_length].tolist())
                    out_latent_dims.append(local_latent_dims[j][i].item())

                outputs.append(output)
            batch_end = time.time()
            latency = float('{:.2f}'.format(batch_end - batch_start))
            print('this batch takes {}s'.format(latency))

        print('writing out...')
        with open(clargs.output_file, 'w') as f:
            json.dump({'outputs': outputs}, f, indent=2)
        print('writing done')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', type=str, nargs=1,
                        help='input data file')
    parser.add_argument('--python_recursion_limit', type=int, default=10000,
                        help='set recursion limit for the Python interpreter')
    parser.add_argument('--save', type=str, required=True,
                        help='directory to load model from')
    parser.add_argument('--output_file', type=str, required=True,
                        help='output file to save psi-related information')
    parser.add_argument('--embedding_file', type=str, default=None, help='word embedding file for keywords')
    clargs = parser.parse_args()
    sys.setrecursionlimit(clargs.python_recursion_limit)
    print(clargs)
    get(clargs)
