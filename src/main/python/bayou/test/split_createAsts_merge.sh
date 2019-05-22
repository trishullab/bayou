#!/usr/bin/env bash

# taking positional parameters
input_file=$1
splits=$2
percentage=$3
echo "input_file is $input_file, splits is $splits, percentage is $percentage"

# set python path
python_path=$(cd ../../../python; pwd)
export PYTHONPATH=${python_path}

# set data stem for this time of experiment
data_stem=${input_file%.*}
echo "original date stem is $data_stem"
now=$(date "+%Y-%m-%d-%H-%M-%S")
echo "current time is $now"
# as _ is a valid naming character
new_data_stem=${data_stem}_${now}_${percentage}

# split data
new_input_file=${new_data_stem}.json
cp ${input_file} ${new_input_file}
# splitted files naming format: '{}-{:02d}.json'.format(args.input_file[0][:-5], i)
python3 ../../scripts/split.py ${new_input_file} --splits ${splits}
echo "split files into $splits pieces"
rm -f ${new_input_file}
echo "remove the copied input file $new_input_file"
splits_dir=${new_data_stem}_splits/
mkdir ${splits_dir}
mv ${new_data_stem}-* ${splits_dir}
echo "move splits into $splits_dir"

# create asts with multiple processes
asts_dir=${new_data_stem}_asts/
mkdir ${asts_dir}
counter=0
for split_file in ${splits_dir}/*.json; do
    python3 -u new_ast_quality_perf_test.py ${split_file} \
        --save /home/lq4/models/comm_of_acm/save \
        --percentage ${percentage} \
        --output_file ${asts_dir}/out_asts_$((${counter} + 1)).json 2>&1 &
    ((counter++))
done

# wait for processes to finish
echo "wait for processes to finish..."
for idx in $(seq 1 $counter); do
    while [ ! -f out_asts/out_asts_${idx}.json ]; do
        echo ${idx}
        sleep 5
    done
done
echo "finish all processes"
rm -rf ${splits_dir}


# merge individual ast files
output_file=${new_data_stem}-merged-out.json
python3 ../../scripts/merge_by_folder.py ${asts_dir} --output_file ${output_file}
echo "finish merging ast files"
rm -rf ${asts_dir}

#compute metrics
#echo "Computing metrics..."
#log=OUT-$output_file_name-$now.txt
## default --top is 3
#java -jar ast_quality_perf_test-1.0-jar-with-dependencies.jar -f merged.json --metric equality-ast --top 10 >> $log 2>&1
#cat $log

end_time=$(date "+%Y-%m-%d-%H-%M-%S")
echo "execution time ranges from $now to $end_time"

