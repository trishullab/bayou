# Bayou
Bayou is a data-driven program synthesis system for Java that uses learned Bayesian specifications for efficient synthesis.

(Coming up: link to arXiv paper on Bayou)

## Requirements
- JDK 1.8
- Python3 (Tested with 3.5.1)
- [Tensorflow](https://www.tensorflow.org) (Tested with 1.0)

## Setup
```
export PYTHONPATH=/path/to/bayou/src/ml
cd /path/to/bayou/src/pl
ant
```

If you are working with the Android SDK,
```
export CLASSPATH=/path/to/android.jar
```
(Bayou has an android.jar from Android 24 under the lib directory if needed)

After setup, run tests to ensure everything is fine:
```
cd scripts
python3 test_driver.py
```

## Usage
To run the driver on a Java program and produce ASTs in the DSL, you would need two things:

- The Java program: this can be any arbitrary parseable .java file, preferably containing a single class. To construct the AST(s), Bayou will first start with the constructors of the class (if any), and then look for public methods in the class (if any). Bayou is interprocedural within the class, meaning any internal methods called will be handled automatically.

- The config file: this is a JSON file containing a set of configuration options for Bayou. A required JSON entity is "api-classes", which is a list of classes (qualified non-generic class names) that Bayou should consider when generating ASTs. API calls to any method in a class not in this list will be considered irrelevant. Another configurable option is "num-unrolls", which controls the number of loop unrolls during sequence extraction from ASTs.

Use the following command to run the driver on Program.java with the config file config.json:
```
java -jar /path/to/bayou/src/pl/out/artifacts/driver/driver.jar -f Program.java -c config.json [-o output.json]
```
The -o option can be used to print the output ASTs to a JSON file.
