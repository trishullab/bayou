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


PS_OPS = [
    'Variable', 'VariableV2', 'AutoReloadVariable', 'MutableHashTable',
    'MutableHashTableOfTensors', 'MutableDenseHashTable'
]
import tensorflow as tf
from bayou.models.low_level_evidences.model import Model
from bayou.models.low_level_evidences.utils import get_available_gpus


class MultiGPUModel():
    def __init__(self, config, iterator, infer=False):

        opt = tf.train.AdamOptimizer(config.learning_rate)

        devices = get_available_gpus()
        controller = '/cpu:0'

        tower_grads = []
        # losses, KL_loss, gen_loss = [], [], []
        losses, ev_losses, latent_losses, gen_losses = [], [], [], []

        self.gpuModels = [None for i in range(len(devices))]
        with tf.variable_scope(tf.get_variable_scope()) as outer_scope:
            for i, Id in enumerate(devices):
                name = 'tower_{}'.format(i)
                # Use the assign_to_device function to ensure that variables are created on the controller.
                with tf.device(self.assign_to_device(Id, controller)), tf.name_scope(name):
                    self.gpuModels[i] = Model(config, iterator, infer=infer)
                    with tf.name_scope("compute_gradients"):
                        grads = opt.compute_gradients(self.gpuModels[i].loss)
                        tower_grads.append(grads)
                    losses.append(self.gpuModels[i].loss)
                    # KL_loss.append(self.gpuModels[i].KL_loss)
                    # gen_loss.append(self.gpuModels[i].gen_loss)
                    ev_losses.append(self.gpuModels[i].evidence_loss)
                    latent_losses.append(self.gpuModels[i].latent_loss)
                    gen_losses.append(self.gpuModels[i].gen_loss)
                # After the first iteration, we want to reuse the variables.
                outer_scope.reuse_variables()

            # Apply the gradients on the controlling device
        with tf.name_scope("apply_gradients"), tf.device(controller):
            # Note that what we are doing here mathematically is equivalent to returning the
            # average loss over the towers and compute the gradients relative to that.
            # Unfortunately, this would place all gradient-computations on one device, which is
            # why we had to compute the gradients above per tower and need to average them here.

            # This function is defined below; it takes the list of (gradient, variable) lists
            # and turns it into a single (gradient, variables) list.
            gradients = self.average_gradients(tower_grads)
            global_step = tf.train.get_or_create_global_step()
            self.apply_gradient_op = opt.apply_gradients(gradients, global_step)
            self.avg_loss = tf.reduce_mean(losses)
            # self.avg_KL_loss = tf.reduce_mean(KL_loss)
            # self.avg_gen_loss = tf.reduce_sum(gen_loss)
            self.avg_gen_loss = tf.reduce_mean(gen_losses)
            self.avg_latent_loss = tf.reduce_mean(latent_losses)
            self.avg_evidence_loss = tf.reduce_mean(ev_losses)

        # var_params = [np.prod([dim.value for dim in var.get_shape()])
        #               for var in tf.trainable_variables()]
        # print('Model parameters: {}'.format(np.sum(var_params)))
        return

    # Source:
    # https://github.com/tensorflow/models/blob/master/tutorials/image/cifar10/cifar10_multi_gpu_train.py#L101
    def average_gradients(self, tower_grads):
        """Calculate the average gradient for each shared variable across all towers.
        Note that this function provides a synchronization point across all towers.
        Args:
        tower_grads: List of lists of (gradient, variable) tuples. The outer list ranges
            over the devices. The inner list ranges over the different variables.
        Returns:
                List of pairs of (gradient, variable) where the gradient has been averaged
                across all towers.
        """
        average_grads = []
        for grad_and_vars in zip(*tower_grads):
            # print(grad_and_vars)
            # Note that each grad_and_vars looks like the following:
            #   ((grad0_gpu0, var0_gpu0), ... , (grad0_gpuN, var0_gpuN))
            grads = [g for g, _ in grad_and_vars]
            grads = tf.stack(grads, axis=0)
            grad = tf.reduce_mean(grads, axis=0)

            # Keep in mind that the Variables are redundant because they are shared
            # across towers. So .. we will just return the first tower's pointer to
            # the Variable.
            v = grad_and_vars[0][1]
            grad_and_var = (grad, v)
            average_grads.append(grad_and_var)
        return average_grads

    # see https://github.com/tensorflow/tensorflow/issues/9517
    def assign_to_device(self, device, ps_device):
        """Returns a function to place variables on the ps_device.

        Args:
            device: Device for everything but variables
            ps_device: Device to put the variables on. Example values are /GPU:0 and /CPU:0.

        If ps_device is not set then the variables will be placed on the default device.
        The best device for shared varibles depends on the platform as well as the
        model. Start with CPU:0 and then test GPU:0 to see if there is an
        improvement.
        """

        def _assign(op):
            node_def = op if isinstance(op, tf.NodeDef) else op.node_def
            if node_def.op in PS_OPS:
                return ps_device
            else:
                return device

        return _assign
