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

import os
import tensorflow as tf
import cProfile

from ast_server import _generate_asts # for code in cProfile.run
from bayou.core.infer import BayesianPredictor


if __name__ == '__main__':

    #
    # Get the path to src/test/resources/model
    #
    file_path = os.path.realpath(__file__)
    pert_tests_path =  os.path.abspath(os.path.join(file_path, os.pardir))
    python_path = os.path.abspath(os.path.join(pert_tests_path, os.pardir))
    test_path =  os.path.abspath(os.path.join(python_path, os.pardir))
    resources_path = os.path.abspath(os.path.join(test_path, "resources"))
    model_path = os.path.abspath(os.path.join(resources_path, "model"))

    #
    # Profile _generate_asts(...)
    #
    with tf.Session() as sess:
        predictor = BayesianPredictor(model_path, sess)
        evidence = '{ "apicalls": [ "setTitle", "setMessage" ], "types": [ "AlertDialog" ], "context": [] }'
        asts = ''
        cProfile.run('asts = _generate_asts(evidence, predictor)', sort='cumtime')
        # print(asts) # if you want to verify reasonable asts were generated
