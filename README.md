# datasyn-nn
Latent Dirichlet Allocation (LDA) over a given set of API methods (words)
Tensorflow implementation of top-down tree LSTM, that learns patterns over [DSL](https://bitbucket.org/vijayaraghavan-murali/datasyn-dsl) trees, based on the paper [Top-down Tree Long Short-Term Memory Networks](http://aclweb.org/anthology/N/N16/N16-1035.pdf).

Inspired from [char-rnn-tensorflow](https://github.com/sherjilozair/char-rnn-tensorflow).

# Requirements
- Python3 (Tested with 3.5.1)
- [sklearn](http://scikit-learn.org/stable) for LDA (Tested with 0.18.1)
- [Tensorflow](https://www.tensorflow.org) (Tested with 0.12.0rc1)

# Basic Usage
Run `python3 lda.py --input_file DATA.json` and follow instructions to run LDA and produce topic distributions for each AST in the dataset DATA.json (see DSL for more details on how to generate this).

Run `python3 train.py DATA.json` to train the model on the DSL ASTs represented in JSON.

Once model is trained, run `python3 predict.py` to get a prediction for the next AST node in a given path. Use the --topic argument to provide a topic distribution.
