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

OS="$(uname)"

if [ $OS -ne "Linux"]; then
	echo "Building binary release only currently supported on Linux."
	exit
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$SCRIPT_DIR/../build_scripts/install_dependencies.sh # build_scripts/build.sh is a substep so ensure all deps met for that script

apt-get update
apt-get install maven zip
