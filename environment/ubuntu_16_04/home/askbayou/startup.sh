#!/bin/bash

export EC2_INSTANCE_ID=$(ec2metadata --instance-id)

cd /home/askbayou/github/pliny/pliny-rest-services
./startPlinyRestServices.sh

cd /home/askbayou/github/pliny/pliny-internal-services
./startApiSynthesisServer.sh

export PYTHONPATH=/home/askbayou/bitbucket/bayou/src/ml
cd /home/askbayou/bitbucket/bayou/scripts/
/usr/bin/python3 ./server.py --save_dir /home/askbayou/bitbucket/bayou/demo/trained
