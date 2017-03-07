from __future__ import print_function
import os
import json
import subprocess

# list of tests that use the Android API
android = [ '27f' ]

testdir = '../test/pl/driver'
outdir = '../src/pl/out/artifacts/driver'
passed = 0
failed = 0

class bcolors:
    OK = '\033[92m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'

def printOK(p):
    global passed
    print(bcolors.OK + '[OK]' + bcolors.ENDC + p, end='')
    passed += 1

def printFAIL(p):
    global failed
    print(bcolors.FAIL + '[FAIL]' + bcolors.ENDC + p, end='')
    failed += 1

for f in os.listdir('../test/pl/driver'):
    if not f.endswith('.java'):
        continue
    c = 'config2.json' if f[:-5] in android else 'config1.json'
    with open(os.path.join(testdir, f)) as fp:
        content = fp.readline()[2:]
    try:
        process = subprocess.run(['java',
                      '-jar', os.path.join(outdir, 'driver.jar'),
                      '-f', os.path.join(testdir, f),
                      '-c', os.path.join(testdir, c)],
                  stdout=subprocess.PIPE, check=True)
    except subprocess.CalledProcessError:
        printFAIL(content)
        continue
    output = process.stdout.decode('utf-8')
    o = os.path.join(testdir, f[:-6] + 'o.json')
    if os.path.getsize(o) == 0: # empty output expected
        if not output == '':
            printFAIL(content)
            continue

    # compare ASTs in JSON
    js = json.loads('{"programs": [' + output + ']}')
    with open(o) as fp:
        jso = json.loads('{"programs": [' + fp.read() + ']}')
    try:
        assert len(jso['programs']) == len(js['programs'])
        assert all([expected['ast'] == out['ast'] for expected, out in
                                     zip(jso['programs'], js['programs'])])
    except AssertionError:
        printFAIL(content)
        continue
    printOK(content)

print('{}/{} tests passed, {} failed'.format(passed, passed+failed, failed))
