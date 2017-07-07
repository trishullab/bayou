ls#!/bin/bash

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

apt-get update
apt-get install openjdk-8-jdk maven zip

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ../../maven_3_3_9/bayou
mvn clean package
cp target/bayou-1.0.0-jar-with-dependencies.jar $SCRIPT_DIR
cp -r ../../../src/main/python $SCRIPT_DIR
cp -r ../../../src/main/resources $SCRIPT_DIR
cp ../../../src/main/bash/binary_release/ubuntu_16_4/*.sh $SCRIPT_DIR
cp -r target/bayou-1.0.0-jar-with-dependencies.jar $SCRIPT_DIR
cp -r ../../../example_inputs $SCRIPT_DIR
cd $SCRIPT_DIR
mv bayou-1.0.0-jar-with-dependencies.jar bayou-1.0.0.jar
zip -r bayou-1.0.0-ubuntu-16.01.zip bayou-1.0.0.jar example_inputs install_dependencies.sh start_bayou.sh synthesize.sh python resources
rm -r bayou-1.0.0.jar example_inputs install_dependencies.sh start_bayou.sh synthesize.sh python resources 
