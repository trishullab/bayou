#!/bin/bash

dir=~/Work/datasyn/nn/data/big

# disable GPU (this is only inference, and GPU runs out of memory if run in parallel)
export CUDA_VISIBLE_DEVICES=

# encdec
python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/encdec \
    --model encdec --n 10 --infer_seqs all --output_file encdec-all.json > encdec-all.txt 2>&1 &
python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/encdec \
    --model encdec --n 10 --infer_seqs half --output_file encdec-half.json > encdec-half.txt 2>&1 &
python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/encdec \
    --model encdec --n 10 --infer_seqs one --output_file encdec-one.json > encdec-one.txt 2>&1 &

# variational
python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/variational \
    --model variational --n 10 --infer_seqs all --output_file variational-all.json > variational-all.txt 2>&1 &
python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/variational \
    --model variational --n 10 --infer_seqs half --output_file variational-half.json > variational-half.txt 2>&1 &
python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/variational \
    --model variational --n 10 --infer_seqs one --output_file variational-one.json > variational-one.txt 2>&1 &
