#!/bin/sh

read -p 'model id: ' id

if [ -d model-$id/ ]; then
        exit
fi

if [ ! -f model-$id.tar.gz ]; then
    aws s3 cp s3://vijay-bayou-data/models/model-$id.tar.gz .
fi

rm -rf model-$id/
mkdir -p model-$id/
tar -xvzf model-$id.tar.gz -C model-$id/
rm -rf model-$id.tar.gz
exit
