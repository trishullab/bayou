#!/usr/bin/env bash

input_file=$1

metrics="equality-ast
	jaccard-sequences
	jaccard-api-calls
	num-statements
	num-control-structures"

data_stem=${input_file%.*}
now=$(date "+%Y-%m-%d-%H-%M-%S")
log=${data_stem}_${now}.log
echo "save the computation results of $input_file to log file $log"
echo ${input_file} >> ${log}

echo "compute metrics"
for metric in $metrics; do
    echo "computing for $metric"
    (echo "$metric: " ; java -jar ast_quality_perf_test-1.0-jar-with-dependencies.jar
        -f ${input_file} --metric ${metric} --top 10)>> ${log} 2>&1
done

echo "done!!!"

