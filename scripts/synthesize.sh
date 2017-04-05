#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: synthesize.sh Program.java"
    exit
fi

if [ ! -f $1 ]; then
    echo "File $1 not found"
    exit 1
fi

t_start=`date +%s`

export BAYOU_HOME=/Users/vijay/Work/bayou
export BAYOU_SERVER=/Users/vijay/Work/bayou/demo/server
export CLASSPATH=$BAYOU_HOME/src/pl/out/production/pl:$BAYOU_HOME/src/pl/lib/android.jar
export PYTHONPATH=$BAYOU_HOME/src/ml

input=$(java -jar $BAYOU_HOME/src/pl/out/artifacts/annotations/evidence_extractor.jar -f $1)
if [ $? -ne 0 ]; then
    echo "ERROR: ensure evidence format is correct."
    exit 1
fi

outpipe=out$RANDOM
echo "Querying the model..."
echo -e "$outpipe#$input" > $BAYOU_SERVER/bayoupipe

maxsleep=10 # seconds
sleeptime=0
grep "asts" -m 1 $BAYOU_SERVER/$outpipe > /dev/null 2>&1
while [ ! $? -eq 0 ]; do
    if [ $sleeptime -gt $maxsleep ]; then
        echo "Inference timeout reached ($maxsleep seconds). Is server running?"
        exit 1
    fi
    sleep 1
    sleeptime=$((sleeptime+1))
    grep "asts" -m 1 $BAYOU_SERVER/$outpipe > /dev/null 2>&1
done

error=$(grep "ERROR" $BAYOU_SERVER/$outpipe)
if [ $? -eq 0 ]; then
    cat $BAYOU_SERVER/$outpipe
    echo ""
    exit 1
fi

echo "Synthesizing..."
output=$(java -jar $BAYOU_HOME/src/pl/out/artifacts/synthesizer/synthesizer.jar -f $1 -a $BAYOU_SERVER/$outpipe 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$output"
    count=$(grep "\"_nodes\"" $BAYOU_SERVER/$outpipe | wc -l)
    t_end=`date +%s`
    echo "Synthesized $count programs in $((t_end - t_start)) seconds"
else
    echo "ERROR: Unexpected error occurred during synthesis. Please try again."
    exit 1
fi
