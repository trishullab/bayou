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


SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD_DIR="${SCRIPT_DIR}/out"

# download model if needed
MODEL_DIR=$SCRIPT_DIR/../../src/main/resources/model/
mkdir -p $MODEL_DIR
python3 $SCRIPT_DIR/fetch_model.py --name model-60-49 --model_dir $MODEL_DIR --url http://sisyphus.cs.rice.edu/release/

# ensure ouput dir is empty
rm -rf $BUILD_DIR
mkdir $BUILD_DIR

# determine version of Bayou being built
cd ../maven_3_3_9/bayou
VER="$(printf 'VERSION=${project.version}\n0\n' | mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate | grep '^VERSION' | cut -c9-)" # get the project version number... e.g 1.1.0

# compile Bayou into a jar file
#    copy Evidence.class between compile and package phase so unit tests run in 2nd phase apply to new class file
#    also copy Bayou.class (@vijay-murali)
mvn clean compile
cp target/classes/edu/rice/cs/caper/bayou/annotations/Evidence.class ../../../src/main/resources/artifacts/classes/edu/rice/cs/caper/bayou/annotations/Evidence.class
cp target/classes/edu/rice/cs/caper/bayou/annotations/Bayou.class ../../../src/main/resources/artifacts/classes/edu/rice/cs/caper/bayou/annotations/Bayou.class
mvn package

# copy and rename post build files into out directory
cp target/bayou-$VER-jar-with-dependencies.jar $BUILD_DIR
cp -r ../../../src/main/python $BUILD_DIR
cp -r ../../../src/main/resources $BUILD_DIR
cp ../../../src/main/bash/binary_release/*.sh $BUILD_DIR
cp -r ../../../doc/external/example_inputs $BUILD_DIR
cd $BUILD_DIR
mv bayou-$VER-jar-with-dependencies.jar bayou-$VER.jar
