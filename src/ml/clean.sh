#!/bin/bash
if [ $# -eq 1 -a -d "$1" ]; then
    dir=$1
    to_delete="config.pkl chars_vocab.pkl checkpoint model.ckpt*"
    for file in ${to_delete}
    do
        rm -f ${dir}/${file}
    done
fi
rm -f *.pyc
rm -f save/*
rm -rf __pycache__
