# datasyn-nn
Tensorflow implementation of top-down tree LSTM, that learns patterns over [DSL](https://bitbucket.org/vijayaraghavan-murali/datasyn-dsl) trees.
Based on the paper [Top-down Tree Long Short-Term Memory Networks](http://aclweb.org/anthology/N/N16/N16-1035.pdf).
Inspired from [char-rnn-tensorflow](https://github.com/sherjilozair/char-rnn-tensorflow).

# Requirements
- [Tensorflow](https://www.tensorflow.org)
- Python 3

# Basic Usage
- Run `python3 train.py <ast.json>` to train the model on the DSL AST represented in JSON (see DSL for more details on how to generate this).

- Once model is trained, run `python3 predict.py` to get a prediction for the next AST node in a given path.
