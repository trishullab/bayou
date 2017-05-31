#!/bin/bash
dir=~/Work/bayou/src/ml/experiments/predict_asts/data+outputs
DATA=DATA-testing-dists.json
arr=(1.0 0.9 0.8 0.7 0.6 0.5 0.4 0.3 0.2 0.1)
for pct in "${arr[@]}" ; do
    python3 extract_topk.py $dir/$DATA --predict_asts_output $dir/variational-$pct.json \
        --k 165 --output_file top5-variational-$pct.json
    python3 extract_topk.py $dir/$DATA --predict_asts_output $dir/encdec-$pct.json \
        --k 165 --output_file top5-encdec-$pct.json
done
