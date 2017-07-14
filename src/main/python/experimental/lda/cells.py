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

from tensorflow.python.ops import array_ops
from tensorflow.python.ops.math_ops import tanh, sigmoid
from tensorflow.python.ops.rnn_cell import RNNCell, _linear
from tensorflow.python.ops import variable_scope as vs

class TopicRNNCell(RNNCell):

    def __init__(self, num_units, activation=tanh):
        self._num_units = num_units
        self._activation = activation

    @property
    def state_size(self):
        return self._num_units

    @property
    def output_size(self):
        return self._num_units

    def __call__(self, inputs, topics, state, scope=None):
        """Topic conditioned RNN: output = new_state = activation(W * input + U * state + T * topics + B)."""
        with vs.variable_scope(scope or type(self).__name__):  # "TopicRNNCell"
            output = self._activation(_linear([inputs, topics, state], self._num_units, True))
        return output, output

class TopicLSTMCell(RNNCell):

    def __init__(self, num_units, forget_bias=1.0, activation=tanh):
        self._num_units = num_units
        self._forget_bias = forget_bias
        self._activation = activation

    @property
    def state_size(self):
        return 2 * self._num_units

    @property
    def output_size(self):
        return self._num_units

    def __call__(self, inputs, topics, state, scope=None):
        with vs.variable_scope(scope or type(self).__name__):  # "TopicLSTMCell"
            c, h = array_ops.split(1, 2, state)
            concat = _linear([inputs, topics, h], 4 * self._num_units, True, scope=scope)

            # i = input_gate, j = new_input, f = forget_gate, o = output_gate
            i, j, f, o = array_ops.split(1, 4, concat)

            new_c = (c * sigmoid(f + self._forget_bias) + sigmoid(i) * self._activation(j))
            new_h = self._activation(new_c) * sigmoid(o)
            new_state = array_ops.concat(1, [new_c, new_h])
            return new_h, new_state
