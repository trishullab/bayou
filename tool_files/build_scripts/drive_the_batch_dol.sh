#!/bin/bash
filename='github-java-files-train-TOP.txt'
filename_parsed='parsed-files.txt'
files_handled_log='files-handled.log'
json_files='json-files.txt'
temp_batch_file='temp-batch.txt'
stderr_file='error.log'

#Delete the Last Log
rm $files_handled_log $stderr_file
# Appends 'java_projects' to beginning of each file_ptr
sed 's/^\./\.\/java_projects/g' $filename > $filename_parsed

#Get the batch_size to feed to batch_dom_driver
Nolines=($(wc -l $filename_parsed))
Steps=($(grep -c ^processor /proc/cpuinfo))
Noiter=$((Nolines / Steps +1))

#Loop over file_ptrs such that all CPU cores are used, with a timeout for unhandled errorrs
for i in $(seq 1 $Noiter);
do
	#Find the start and end file_ptrs
        start=$(((i-1)*Steps+1))
        if [ $i -eq $Noiter ]
        then
                end=$Nolines
        else
                end=$((i*Steps))
        fi
	#store the temp file names somewhere
        sed -n "$start,$end p" "$filename_parsed" > "$temp_batch_file"
	# run the java dom with timeout and redirect both stdout and stderr
        timeout --signal=SIGINT 1m java -jar ~/bayou/tool_files/maven_3_3_9/batch_dom_driver/target/batch_dom_driver-1.0-jar-with-dependencies.jar $temp_batch_file config.json >> $files_handled_log 2>>$stderr_file
	if (( $end % ($Steps*10) == 0 ))
	then
		echo $end;
	fi
done
# Need to stich the jsons after this step
#locate all json files easily by this
sed 's/$/\.json/g' $filename_parsed > $json_files
# Use the prebuilt merge API
python3 bayou/src/main/python/scripts/merge.py $json_files --output_file output.json
#delete temp files
rm $temp_batch_file $json_files $filename_parsed
