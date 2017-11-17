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
OUT_DIR=$SCRIPT_DIR/out

rm -rf $OUT_DIR

$SCRIPT_DIR/../build_binary_release/build.sh
unzip -d $OUT_DIR ../build_binary_release/bayou-*.zip

$OUT_DIR/start_bayou.sh &
sleep 60
python3 accept.py tests/
$OUT_DIR/stop_bayou.sh

sleep 5
rm -rf $OUT_DIR

