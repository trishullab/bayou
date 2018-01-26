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

trap "exit" INT TERM # trap lines make it so taht when this script terminates the background java process does as well
trap "kill 0" EXIT

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

BAYOU_JAR="$(ls $SCRIPT_DIR/*.jar)"
cd $SCRIPT_DIR # log4j treats config paths relataive to current directory.  we need this so logs is next to the jar file and not the directory from where the script is being run

#
# Determine if the JVM vendor is Oracle and, if so, ensure java will be launched with rt_debug.jar loaded as the bootclasspath.  This is done so we can take advantage
# of the extra debug information in re_debug when generating synthesis results. However, this jar is incompatable with openjdk.
#
# See https://github.com/capergroup/bayou/issues/166
#
JAVA_VENDOR="$(java -version 2>&1 | grep -o 'openjdk\|java')"
if [ "$JAVA_VENDOR" == "java" ] # if vendor is Oracle
    then
      BOOT_CLASSPATH="-Xbootclasspath/p:$SCRIPT_DIR/resources/artifacts/jar/rt_debug.jar"
    else
      BOOT_CLASSPATH=""
fi

#
# Start Java front end.
#

java $BOOT_CLASSPATH -DconfigurationFile=$SCRIPT_DIR/resources/conf/apiSynthesisServerConfig.properties -Dlog4j.configurationFile=$SCRIPT_DIR/resources/conf/apiSynthesisServerLog4j2.xml -jar $BAYOU_JAR &

#
# Start Python back end.
#
if [ $# -eq 0 ]
  then
    LOGS_DIR=$SCRIPT_DIR/logs
  else
    LOGS_DIR=$1
fi

export PYTHONPATH=$SCRIPT_DIR/python
python3 $SCRIPT_DIR/python/bayou/server/ast_server.py --save_dir "$SCRIPT_DIR/resources/model" --logs_dir "$LOGS_DIR"
