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

from __future__ import print_function

import boto3
import paramiko
import argparse
import json
import time
import os
import textwrap

HELP = """\
Config options should be given as a JSON file:
{
  "training_id": "some-model-ID",
  "ssh_private_key_file": "/location/of/file/to/login/with/ssh/.pem",
  "ec2_launch_config": {
    "ImageId": "ami-00000000",
    "InstanceType": "p2.xlarge",
    "KeyName": "keyname",
    "SubnetId": "subnet-00000000"
  },
  "ec2_spot_price": "1.00",
  "s3_data_file": "s3://bucket-name/path/to/DATA.json",
  "bayou_git_hash": "master",
  "bayou_patch_file": "/location/of/patch/file (if any, set to null otherwise)",
  "bayou_config": {
    "model": "core",
    "lda_files": [
      "s3://bucket-name/location/of/trained/lda1.tar.gz",
      "s3://bucket-name/location/of/trained/lda2.tar.gz",
      "note: this list is relevant only if model is core"
    ],
    "print_step": 1,
    "alpha": 0,
    "beta": 0,
    "learning_rate": 0.0006,
    "num_epochs": 50,
    "latent_size": 32,
    "batch_size": 50,
    "evidence": [
      {
        "name": "apicalls",
        "units": 64,
        "tile": 1,
        "num_layers": 1
      },
      {
        "name": "types",
        "units": 32,
        "tile": 1,
        "num_layers": 1
      },
      {
        "name": "keywords",
        "units": 64,
        "tile": 1,
        "num_layers": 1
      }
    ],
    "decoder": {
      "units": 128,
      "num_layers": 1,
      "max_ast_depth": 32
    }
  }
}
"""


class message:
    def __init__(self, s):
        self.str = s

    def __enter__(self):
        print(self.str + '...', end='', flush=True)

    def __exit__(self, exc_type, exc_val, exc_tb):
        print('done' if exc_type is None else 'ERROR!')


def check_aws_config():
    home = os.path.expanduser('~')
    creds = os.path.join(os.path.join(home, '.aws'), 'credentials')
    aws_config = os.path.join(os.path.join(home, '.aws'), 'config')
    if not os.path.isfile(creds) or not os.path.isfile(aws_config):
        raise ValueError('credentials/config file not found in home directory/.aws. Have you run aws configure?')
    return creds, aws_config


def request_spot_instance(client, launch_config, spot_price):
    launch = {'LaunchSpecification': launch_config}
    response = client.request_spot_instances(SpotPrice=spot_price, **launch)
    return response['SpotInstanceRequests'][0]['SpotInstanceRequestId']


def get_instance_id_blocking(client, spot_request_id):
    waiter = client.get_waiter('spot_instance_request_fulfilled')
    try:
        waiter.wait(SpotInstanceRequestIds=[spot_request_id], WaiterConfig={'Delay': 10, 'MaxAttempts': 4})
    except:
        with message('Did not get instance id. Closing spot request {}'.format(spot_request_id)):
            client.cancel_spot_instance_requests(SpotInstanceRequestIds=[spot_request_id])
        raise
    response = client.describe_spot_instance_requests()
    for spot_request in response['SpotInstanceRequests']:
        if spot_request['SpotInstanceRequestId'] == spot_request_id:
            return spot_request['InstanceId']
    raise ValueError('Could not find instance id for spot request: {}'.format(spot_request_id))


def terminate_instance_blocking(client, instance_id):
    client.terminate_instances(InstanceIds=[instance_id])
    waiter = client.get_waiter('instance_terminated')
    waiter.wait(InstanceIds=[instance_id], WaiterConfig={'Delay': 10, 'MaxAttempts': 16})


def cancel_spot_request(client, spot_request_id):
    client.cancel_spot_instance_requests(SpotInstanceRequestIds=[spot_request_id])


def get_public_ip(client, instance_id):
    response = client.describe_instances(InstanceIds=[instance_id])
    return response['Reservations'][0]['Instances'][0]['PublicIpAddress']


def connect_to_ip(ssh_private_key_file, ip):
    key = paramiko.RSAKey.from_private_key_file(ssh_private_key_file)
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    for attempts in range(10):
        try:
            ssh.connect(ip, username='ubuntu', pkey=key, timeout=600, auth_timeout=600, banner_timeout=600)
        except TimeoutError:
            print('#{}...'.format(attempts+1), end='', flush=True)
    return ssh


def exec_command_blocking(ssh, command):
    stdin, stdout, stderr = ssh.exec_command(command)
    exit_code = stdout.channel.recv_exit_status()
    if exit_code != 0:
        raise ValueError('exit code is not 0 for command: ' + command)
    return stdout


def start_training(ssh, config):
    model_type = config.bayou_config['model']
    if model_type == 'core':
        model_dir = '~/bayou/src/main/python/bayou/models/core'
        # additional setup of LDA files
        for i, lda_file in enumerate(config.bayou_config['lda_files']):
            exec_command_blocking(ssh, 'aws s3 cp {} lda{}.tar.gz'.format(lda_file, i))
            exec_command_blocking(ssh, 'tar xf lda{}.tar.gz -C save'.format(i))
    elif model_type == 'lle':
        model_dir = '~/bayou/src/main/python/bayou/models/low_level_evidences'
    else:
        raise ValueError('Invalid model type in config: ' + model_type)
    exec_command_blocking(ssh, """
            export LD_LIBRARY_PATH=/usr/local/cuda-8.0/lib64:$LD_LIBRARY_PATH; \
            export LD_LIBRARY_PATH=~/cuda/lib64:$LD_LIBRARY_PATH; \
            export PYTHONPATH=~/bayou/src/main/python;
            python3 -u {}/train.py DATA.json --config config.json --save save > save/train.out 2>&1 &"""
                          .format(model_dir))


def automate_train(config):
    # 1. Do AWS stuff
    with message('Checking AWS config'):
        client = boto3.client('ec2')
        check_aws_config()

    with message('Requesting spot instance'):
        spot_request_id = request_spot_instance(client, config.ec2_launch_config, config.ec2_spot_price)

    with message('Getting the instance id for request {}'.format(spot_request_id)):
        instance_id = get_instance_id_blocking(client, spot_request_id)
        client.create_tags(Resources=[instance_id], Tags=[{'Key': 'Name', 'Value': config.training_id}])

    with message('Getting the public IP for instance {}'.format(instance_id)):
        public_ip = get_public_ip(client, instance_id)

    if os.path.isfile('instances.json'):
        with open('instances.json') as f:
            js = json.load(f)
    else:
        js = {'instances': []}
    with message('Adding entry to instances.json'), open('instances.json', 'w') as f:
        instance = {'training_id': config.training_id,
                    'spot_request_id': spot_request_id,
                    'instance_id': instance_id,
                    'public_ip': public_ip}
        js['instances'].append(instance)
        js['ssh_private_key_file'] = config.ssh_private_key_file
        json.dump(js, fp=f, indent=2)

    # 2. Connect to the instance and setup for training
    with message('Connecting to public IP {}'.format(public_ip)):
        ssh = connect_to_ip(config.ssh_private_key_file, public_ip)

    with message('Copying DATA file {} to instance'.format(config.s3_data_file)):
        exec_command_blocking(ssh, 'aws s3 cp {} DATA.json'.format(config.s3_data_file))

    with message('Checking out Bayou at {}'.format(config.bayou_git_hash)):
        exec_command_blocking(ssh, '(cd bayou; git pull)'.format(config.bayou_git_hash))
        exec_command_blocking(ssh, '(cd bayou; git checkout {})'.format(config.bayou_git_hash))

    sftp = ssh.open_sftp()
    if config.bayou_patch_file is not None:
        with message('Applying patch file {}'.format(config.bayou_patch_file)):
            sftp.put(config.bayou_patch_file, 'bayou/patch')
            exec_command_blocking(ssh, '(cd bayou; git apply patch)')

    with message('Configuring Bayou for training'):
        tmpfile = '.config-{}.json'.format(time.time())
        with open(tmpfile, 'w') as f:
            json.dump(config.bayou_config, fp=f, indent=2)
        sftp.put(tmpfile, 'config.json')
        sftp.mkdir('save')
        os.remove(tmpfile)

    # 3. Start training, doing additional work depending on model type
    with message('Starting training'):
        start_training(ssh, config)


def pingall():
    with open('instances.json') as f:
        js = json.load(f)

    ssh_private_key_file = js['ssh_private_key_file']
    for instance in js['instances']:
        public_ip = instance['public_ip']
        with message('Connecting to public IP {}'.format(public_ip)):
            ssh = connect_to_ip(ssh_private_key_file, public_ip)
        stdout = exec_command_blocking(ssh, 'grep "Model checkpoint" save/train.out')
        out = stdout.readlines()  # 1 MB (output of grep should not be more than this)
        print(instance['training_id'])
        print(''.join(out))
        print()


def wrapup(training_id, checkpoint_epoch, s3_model_location):
    with open('instances.json') as f:
        js = json.load(f)

    ssh_private_key_file = js['ssh_private_key_file']
    instances = [(i, instance) for i, instance in enumerate(js['instances']) if instance['training_id'] == training_id]
    assert len(instances) == 1, 'Instance ID is not unique!'

    idx, instance = instances[0]
    public_ip = instance['public_ip']
    instance_id = instance['instance_id']
    spot_request_id = instance['spot_request_id']

    with message('Checking AWS config'):
        client = boto3.client('ec2')
        check_aws_config()

    with message('Connecting to public IP {}'.format(public_ip)):
        ssh = connect_to_ip(ssh_private_key_file, public_ip)

    with message('Setting checkpoint to epoch {}'.format(checkpoint_epoch)):
        exec_command_blocking(ssh, 'echo "model_checkpoint_path: \"model{}.ckpt\"" > save/checkpoint'
                              .format(checkpoint_epoch))
        exec_command_blocking(ssh, 'echo "all_model_checkpoint_paths: \"model{}.ckpt\"" >> save/checkpoint'
                              .format(checkpoint_epoch))

    with message('Tarballing the model into {}.tar.gz'.format(training_id)):
        exec_command_blocking(ssh, 'tar czf {}.tar.gz -C save .'.format(training_id, training_id))

    with message('Saving to {}'.format(s3_model_location)):
        exec_command_blocking(ssh, 'aws s3 cp {}.tar.gz {}'.format(training_id, s3_model_location))

    with message('Terminating instance {}'.format(instance_id)):
        terminate_instance_blocking(client, instance_id)

    with message('Closing spot request {}'.format(spot_request_id)):
        cancel_spot_request(client, spot_request_id)

    with message('Updating instances.json'):
        del js['instances'][idx]
        with open('instances.json', 'w') as f:
            json.dump(js, fp=f, indent=2)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter,
                                     description=textwrap.dedent(HELP))
    parser.add_argument('--config', type=str, default=None,
                        help='config file (see description above for help)')
    parser.add_argument('--pingall', action='store_true',
                        help='get training status of all instances')
    parser.add_argument('--wrapup', type=str, nargs=3,
                        help='save the given model training ID, with the given epoch number (int) as checkpoint,'
                             ' to the given S3 location, and terminate the instance')
    clargs = parser.parse_args()
    nargs = (1 if clargs.config is not None else 0) + \
            (1 if clargs.pingall else 0) + \
            (1 if clargs.wrapup is not None else 0)
    if nargs != 1:
        parser.print_help()
        parser.error('Provide exactly one argument')

    if clargs.config is not None:
        with open(clargs.config) as f:
            js = json.load(f)
        config = argparse.Namespace()
        for key in js:
            config.__dict__[key] = js[key]
        automate_train(config)
    elif clargs.pingall:
        pingall()
    elif clargs.wrapup is not None:
        wrapup(clargs.wrapup[0], clargs.wrapup[1], clargs.wrapup[2])
