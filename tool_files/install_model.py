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

import urllib.request
import tarfile

localDir  = '../src/main/resources/model/'
localFile = localDir + 'popl18.tar.gz'

print('downloading network...')
urllib.request.urlretrieve('http://release.askbayou.com/popl18.tar.gz', localFile)

print('expanding network...')
tar = tarfile.open(localFile)
tar.extractall(path=localDir)
tar.close()
