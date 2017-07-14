#!/bin/bash

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

dir=~/Work/bayou/src/ml/experiments/predict_asts/data+outputs
DATA=DATA-testing-dists.json
arr=(1.0 0.9 0.8 0.7 0.6 0.5 0.4 0.3 0.2 0.1)
for pct in "${arr[@]}" ; do
    python3 extract_topk.py $dir/$DATA --predict_asts_output $dir/variational-$pct.json \
        --k 165 --output_file top5-variational-$pct.json
    python3 extract_topk.py $dir/$DATA --predict_asts_output $dir/encdec-$pct.json \
        --k 165 --output_file top5-encdec-$pct.json
done
