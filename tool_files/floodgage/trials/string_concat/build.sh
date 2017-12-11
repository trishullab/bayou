#!/bin/bash

rm *.jar 2>/dev/null
mvn -q test clean
mv *.jar trials.jar 2>/dev/null
