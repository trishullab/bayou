# Copyright (c) 2017 rmbar
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import argparse
import json
import os
import subprocess

#
# Constant string prefixes and suffix for printing colors to the console.
#
import sys

HEADER = '\033[95m'
OK_GREEN = '\033[92m'
WARNING = '\033[93m'
FAIL = '\033[91m'
END_COLOR = '\033[0m'


def run_test_from_python_test_file(test_file_path: str):
    """Runs the given python file and returns True if the exit code is 0 and False otherwise.

    test_file_path -- the path to the python file to run
    """

    print(HEADER + "[running: " + test_file_path + "]" + END_COLOR)
    test_file_parent_path = os.path.abspath(os.path.join(test_file_path, os.pardir))
    test_file_name = os.path.basename(test_file_path)
    return run_shell_command("python3 " + test_file_name, test_file_parent_path, 0)


def run_test_from_json_test_file(test_file_path: str):
    """Parses the given json test file and runs the defined test returning True if the tests passes and False otherwise.

    Example json:

    {
      "test_type": "shell command",
      "command" : "echo dog",
      "expect_exit": 0,
      "expect_stdout": "dog\n"
    }

    test_file_path -- the path to the json test file
    """

    print(HEADER + "[running: " + test_file_path + "]" + END_COLOR)

    #
    # Read and parse test_file_path as a JSON file.
    #
    with open(test_file_path, "r") as test_file:
        test_file_content = test_file.read()

    try:
        test = json.loads(test_file_content)
    except json.JSONDecodeError:
        print(WARNING + "non-json test file: " + test_file_path + END_COLOR)
        return False

    #
    # Determine the kind of test to run and run it.
    #
    test_type = test.get('test type', None)
    if test_type is None:
        test_type = test.get('test_type', None)  # support legacy key name

    if test_type is None:
        print(WARNING + "missing test_type in test file: " + test_file_path + END_COLOR)
        return False

    if test_type == "shell command":

        command = test.get('command', None)

        if command is None:
            print(WARNING + "missing command in test file: " + test_file_path + END_COLOR)
            return False

        expect_exit = test.get('expect exit', None)
        if expect_exit is None:
            expect_exit = test.get('expect_exit', None)  # support legacy key name

        expect_stdout = test.get('expect stdout', None)
        if expect_stdout is None:
            expect_stdout = test.get('expect_stdout', None)  # support legacy key name

        expect_stdout_contains = test.get('expect stdout contains', None)

        print(HEADER + "shell command: " + command + END_COLOR)

        test_file_parent_path = os.path.abspath(os.path.join(test_file_path, os.pardir))
        return run_shell_command(command, test_file_parent_path, expect_exit, expect_stdout, expect_stdout_contains)
    else:
        print(WARNING + "unknown test type in test file: " + test_file_path + END_COLOR)
        return False


def run_shell_command(command: str, working_directory: str, expected_exit: int, expected_stdout: str = None,
                      expect_stdout_contains: str = None):
    """Runs the given command string as a shell command in a new subprocess and returns whether the command
       met the given expectations.

    command -- the shell command to run e.g. "ls -l"
    working_directory -- the working directory of the launched shell
    expected_exit -- the expected exit code of the shell command or None for no expectation
    expected_stdout -- the expected standard out characters printed by the shell command or None for no expectation
    expect_stdout_contains -- an expected substring of standard out characters printed by the shell command or None for no
                              expectation
    """

    #
    # Run the given shell command and report standard out.
    #
    completed_process = subprocess.run(command, shell=True, stdout=subprocess.PIPE, cwd=working_directory)
    std_out = completed_process.stdout.decode('utf-8') # take stdout bytes and assume UTF-8 text

    if len(std_out) > 0:
        print(HEADER + "<begin stdout>" + END_COLOR + completed_process.stdout.decode('utf-8') +
              HEADER + "<end stdout>" + END_COLOR)

    #
    # Check if the exit code and standard out match expectations if specified.
    #
    test_passed = True

    if expected_stdout is not None and expected_stdout != std_out:
        test_passed = False
        print(FAIL + "<expected out>" + END_COLOR + expected_stdout + FAIL + "<end expected out>" + END_COLOR)
        # n.b. we use the string "<expected out>" instead of "<expected stdout>" so same char length as "<begin stdout>"
        # and thus lines up visually.

    if expect_stdout_contains is not None and expect_stdout_contains not in std_out:
        test_passed = False
        print(FAIL + "<expected stdout to contain>" + END_COLOR + expect_stdout_contains + FAIL +
              "<end expected stdout to contain>" + END_COLOR)

    if expected_exit is not None and expected_exit != completed_process.returncode:
        test_passed = False
        print(FAIL + "<expected error code " + str(expected_exit) + " but found " + str(completed_process.returncode) +
              ">" + END_COLOR)

    return test_passed


def include_file(file_path: str, ignore_py: bool):
    """Returns whether the given file path should be considered a test file for this execution.

    file_path -- the path of the file in the tests path
    ignore_py -- whether files that in in .py should be considered a test file
    """
    return file_path.endswith(".test") or (file_path.endswith(".py") and not ignore_py)

if __name__ == "__main__":

    print(HEADER)
    print("#########################")
    print("   Welcome to AcceptPy   ")
    print("#########################")
    print(END_COLOR)

    #
    # Determine the tests directory path from the command line arguments.
    #
    parser = argparse.ArgumentParser()
    parser.add_argument('tests_path', type=str, help='the directory containing tests to run')
    parser.add_argument('--ignore_py', dest='ignore_py', action='store_const', const=True, default=False,
                        help='do not treat found .py files in the tests path as tests')
    args = parser.parse_args()
    tests_path = args.tests_path

    #
    # Find all test files.
    #
    print("searching for tests...")
    print("")
    test_file_paths = []
    if os.path.isfile(tests_path) and include_file(tests_path, args.ignore_py):
        test_file_paths.append(tests_path)
        print(tests_path)
    else:
        for root, dirs, files in os.walk(tests_path):
            for file in files:
              if include_file(file, args.ignore_py):
                f = os.path.join(root, file)
                print(f)
                test_file_paths.append(f)

    print("")
    print("found " + str(len(test_file_paths)) + " tests.")
    print("")

    #
    # Run each test file.
    #
    failed_test_files_paths = []

    i = 1
    for file in test_file_paths:
        print(HEADER)
        print("#########################")
        print("   Test " + str(i) + " of " + str(len(test_file_paths)))
        print("#########################\n" + END_COLOR)
        if file.endswith(".test"):
            passed = run_test_from_json_test_file(file)
        else:
            passed = run_test_from_python_test_file(file)

        if passed:
            print(OK_GREEN + "[PASSED]\n" + END_COLOR)
        else:
            failed_test_files_paths.append(file)
            print(FAIL + "[FAILED]\n" + END_COLOR)

        i = i + 1

    print(HEADER)
    print("#########################")
    print("   Results:")
    print("#########################\n" + END_COLOR)

    #
    # Report end of run statistics to user.
    #
    if len(failed_test_files_paths) > 0:
        print("failed tests:")

        for failed_test in failed_test_files_paths:
            print(failed_test)

    if len(failed_test_files_paths) > 0:
        print(FAIL)
    else:
        print(OK_GREEN)

    print(str(len(test_file_paths) - len(failed_test_files_paths)) + " of " + str(len(test_file_paths)) +
          " tests passed.")
    print(END_COLOR)

    #
    # Exit.
    #
    if len(failed_test_files_paths) > 0:
        sys.exit(1)
    else:
        sys.exit(0)
