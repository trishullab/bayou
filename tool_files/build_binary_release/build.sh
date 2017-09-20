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

$SCRIPT_DIR/../build_scripts/build.sh

cd ../maven_3_3_9/bayou
VER="$(printf 'VERSION=${project.version}\n0\n' | mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate | grep '^VERSION' | cut -c9-)" # get the project version number... e.g 1.1.0 mvn clean package

#mvn clean package
#cp target/bayou-$VER-jar-with-dependencies.jar $SCRIPT_DIR
#cp -r ../../../src/main/python $SCRIPT_DIR
#cp -r ../../../src/main/resources $SCRIPT_DIR
#cp ../../../src/main/bash/binary_release/*.sh $SCRIPT_DIR
#cp -r target/bayou-$VER-jar-with-dependencies.jar $SCRIPT_DIR
#cp -r ../../../example_inputs $SCRIPT_DIR
#cd $SCRIPT_DIR
#mv bayou-$VER-jar-with-dependencies.jar bayou-$VER.jar
#echo $VER
cd $SCRIPT_DIR/../build_scripts/out/
zip -r $SCRIPT_DIR/bayou-$VER.zip *
#rm -r bayou-$VER.jar example_inputs install_dependencies_apt.sh install_dependencies_mac.sh start_bayou.sh synthesize.sh python resources
