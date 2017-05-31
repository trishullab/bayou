#!/bin/bash

dir=~/Work/datasyn/nn/data/big

# disable GPU (this is only inference, and GPU runs out of memory if run in parallel)
export CUDA_VISIBLE_DEVICES=

arr=(1.0 0.9 0.8 0.7 0.6 0.5 0.4 0.3 0.2 0.1)
for pct in "${arr[@]}" ; do
    python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/encdec \
        --model encdec --n 10 --infer_seqs ${pct} --output_file encdec-${pct}.json > encdec-${pct}.txt 2>&1 &
    python3 -u predict_asts.py $dir/DATA-testing.json --save_dir $dir/trained/variational \
        --model variational --n 10 --infer_seqs ${pct} --output_file variational-${pct}.json > variational-${pct}.txt 2>&1 &
done
