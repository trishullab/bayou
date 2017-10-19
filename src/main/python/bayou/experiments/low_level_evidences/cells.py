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


class MultiLayerComposedRNNCell(tf.nn.rnn_cell.RNNCell):
    def __init__(self, cells):
        super(MultiLayerComposedRNNCell, self).__init__()

        # check if all cells are the same size
        assert len(cells) > 0
        states = [cell.state_size for cell in cells]
        outputs = [cell.output_size for cell in cells]
        assert states.count(states[0]) == len(states)
        assert outputs.count(outputs[0]) == len(outputs)

        self.cells = cells

    @property
    def state_size(self):
        return self.cells[-1].state_size

    @property
    def output_size(self):
        return self.cells[-1].output_size

    def call(self, inputs, state):
        for i, cell in enumerate(self.cells):
            with tf.variable_scope('layer{}'.format(i)):
                outputs, state = cell(inputs if i == 0 else outputs, state)
        return outputs, state
