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

import argparse
import os
import shutil
import zipfile
import urllib.request

if __name__ == '__main__':

    # Parse command line args.
    parser = argparse.ArgumentParser()
    parser.add_argument('--name', type=str, required=True)
    parser.add_argument('--url',  type=str, required=True)
    parser.add_argument('--model_dir',  type=str, required=True)
    args = parser.parse_args()

    model_name=args.name
    model_dir=args.model_dir
    fetch_url_base=args.url

    # Ensure the model directory exists prior to proceeding.
    if not os.path.exists(model_dir) or not os.path.isdir(model_dir):
        print("No such dir: " + model_dir)
        exit(1)

    model_file = os.path.join(model_dir, model_name + ".ckpt.index")

    # Check if we already have the model.
    if os.path.exists(model_file):
        exit(0)

    # We don't have the model. Delete any existing files (probably old model files).
    shutil.rmtree(model_dir)
    os.makedirs(model_dir)

    # Download new model files from remote server.
    fetch_url = fetch_url_base + model_name + ".zip"
    model_zip_file = os.path.join(model_dir,  model_name + ".zip")
    print("Downloading " + fetch_url)
    urllib.request.urlretrieve(fetch_url, model_zip_file)

    # Unzip model files and delete ziped model.
    with zipfile.ZipFile(model_zip_file,"r") as zip_ref:
        zip_ref.extractall(model_dir)

    os.unlink(model_zip_file)

    

