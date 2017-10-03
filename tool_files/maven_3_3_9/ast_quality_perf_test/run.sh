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

now=$(date "+%Y-%m-%d-%H-%M-%S")

echo "Building Bayou..."
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD_DIR="${SCRIPT_DIR}/target"

rm -rf $BUILD_DIR

mvn clean package
if [ $? -ne 0 ]; then
    exit
fi

cp -r ../../../src/main/python $BUILD_DIR
cp -r ../../../src/main/resources $BUILD_DIR
export PYTHONPATH=$BUILD_DIR/python
mkdir -p DATA
cd DATA

check_and_download() {
    file=$1
    link=$2
    if [ ! -f $file ]; then
        echo "Downloading $file..."
        wget $link
    fi
}

echo "Downloading file list..."
wget https://www.dropbox.com/s/z77lspu13v2akqp/android.txt -O android.txt
while read line; do
    file=$(echo $line | cut -f1 -d' ')
    link=$(echo $line | cut -f2 -d' ')
    check_and_download $file $link
done < android.txt

evidence="apicalls
types
context"

all="sampled
sampled-max3
sampled-max2
sampled-max1"

echo "Starting inference query processes..."
for ev in $evidence; do
    if [ ! -f DATA-$ev.json ]; then
        python3 -u $BUILD_DIR/python/bayou/test/ast_quality_perf_test.py DATA-testing.json \
            --save $BUILD_DIR/resources/model \
            --evidence $ev \
            --output_file DATA-$ev.json 2>&1 &
    else
        echo "Using the DATA-$ev.json that already exists. Remove it if you want to re-compute ASTs."
    fi
done
for ev in $all; do
    if [ ! -f DATA-$ev.json ]; then
        python3 -u $BUILD_DIR/python/bayou/test/ast_quality_perf_test.py DATA-testing-$ev.json \
            --save $BUILD_DIR/resources/model \
            --evidence all \
            --output_file DATA-$ev.json 2>&1 &
    else
        echo "Using the DATA-$ev.json that already exists. Remove it if you want to re-compute ASTs."
    fi
done

echo "Waiting for processes to finish..."
for ev in $evidence $all; do
    while [ ! -f DATA-$ev.json ]; do
        sleep 10
    done
done

metrics="latency
equality-ast
jaccard-sequences
jaccard-api-calls
num-statements
num-control-structures"

echo "Computing metrics..."
log=OUT-$now.txt
for metric in $metrics; do
    for ev in $evidence $all; do
        echo -n "$metric $ev: " >> $log
        java -jar $BUILD_DIR/ast_quality_perf_test-1.0-jar-with-dependencies.jar \
            -f DATA-$ev.json --metric $metric --top 3 >> $log 2>&1
    done
done

cat $log
echo "Metrics saved to $log"
