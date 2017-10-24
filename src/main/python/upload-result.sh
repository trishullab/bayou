#!/bin/sh

read -p 'output directory name (no / postfix) under s3://letao/bayou-validation-results: ' model_dir
ls *.json
read -p 'merged out asts file: ' merged_file
ls *.txt
read -p 'result metric file: ' result_file

if [ -f $merged_file ]; then
    aws s3 cp $merged_file s3://letao/bayou-validation-results/$model_dir/
else
    echo "merged out asts file $merged_file doesn't exist"
fi

if [ -f $result_file ]; then
    aws s3 cp $result_file s3://letao/bayou-validation-results/$model_dir/
else
    echo "result metric file $result_file doesn't exist"
fi

exit
