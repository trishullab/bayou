# Modified from Tensorflow's seq2seq.py
import tensorflow as tf
from tensorflow.python.framework import dtypes
from tensorflow.python.framework import ops
from tensorflow.python.ops import array_ops
from tensorflow.python.ops import embedding_ops
from tensorflow.python.ops import math_ops
from tensorflow.python.ops import nn_ops
from tensorflow.python.ops import variable_scope

def _extract_argmax_and_embed(embedding, output_projection=None,
                              update_embedding=True):
  def loop_function(prev, _):
    if output_projection is not None:
      prev = nn_ops.xw_plus_b(
          prev, output_projection[0], output_projection[1])
    prev_symbol = math_ops.argmax(prev, 1)
    emb_prev = embedding_ops.embedding_lookup(embedding, prev_symbol)
    if not update_embedding:
      emb_prev = array_ops.stop_gradient(emb_prev)
    return emb_prev
  return loop_function


def rnn_decoder(decoder_inputs, edge_inputs, topics, initial_state, cell1, cell2, loop_function=None,
                scope=None):
  with variable_scope.variable_scope(scope or "rnn_decoder"):
    state = initial_state
    outputs = []
    prev = None
    for i, inp in enumerate(decoder_inputs):
      if loop_function is not None and prev is not None:
        with variable_scope.variable_scope("loop_function", reuse=True):
          inp = loop_function(prev, i)
      if i > 0:
        variable_scope.get_variable_scope().reuse_variables()
      with variable_scope.variable_scope('cell1'): # cell1 handles CHILD_EDGE
        output1, state1 = cell1(inp, topics[i], state)
      with variable_scope.variable_scope('cell2'): # cell2 handles SIBLING_EDGE (LEAF_EDGE is dummy for synthesis)
        output2, state2 = cell2(inp, topics[i], state)
      output = tf.select(edge_inputs[i], output1, output2)
      state = tf.select(edge_inputs[i], state1, state2)
      outputs.append(output)
      if loop_function is not None:
        prev = output
  return outputs, state


def embedding_rnn_decoder(decoder_inputs,
                          edge_inputs,
                          topics,
                          initial_state,
                          cell1,
                          cell2,
                          num_symbols,
                          embedding_size,
                          output_projection=None,
                          feed_previous=False,
                          update_embedding_for_previous=True,
                          scope=None):
  with variable_scope.variable_scope(scope or "embedding_rnn_decoder") as scope:
    if output_projection is not None:
      proj_weights = ops.convert_to_tensor(output_projection[0], dtype=dtypes.float32)
      proj_weights.get_shape().assert_is_compatible_with([None, num_symbols])
      proj_biases = ops.convert_to_tensor(output_projection[1], dtype=dtypes.float32)
      proj_biases.get_shape().assert_is_compatible_with([num_symbols])

    embedding = variable_scope.get_variable("embedding",
                                            [num_symbols, embedding_size])
    loop_function = _extract_argmax_and_embed(
        embedding, output_projection,
        update_embedding_for_previous) if feed_previous else None
    emb_inp = (
        embedding_ops.embedding_lookup(embedding, i) for i in decoder_inputs)
    return rnn_decoder(emb_inp, edge_inputs, topics, initial_state, cell1, cell2,
                       loop_function=loop_function)
