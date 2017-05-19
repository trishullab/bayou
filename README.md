# Bayou
Bayou is a data-driven program synthesis system for Java that uses learned Bayesian specifications for efficient synthesis.

[arXiv](https://arxiv.org/abs/1703.05698) paper on Bayou.

There are three main modules in Bayou:
- [driver](https://github.com/capergroup/bayou/tree/master/src/pl/src/edu/rice/bayou/dom_driver): extracts sketches (in the [DSL](https://github.com/capergroup/bayou/tree/master/src/pl/src/edu/rice/bayou/dsl)) and evidences from a Java program to generate the training data
- [model](https://github.com/capergroup/bayou/tree/master/src/ml/bayou): implements the BED neural network (see paper), word embeddings, their training and inference procedures
- [synthesizer](https://github.com/capergroup/bayou/tree/master/src/pl/src/edu/rice/bayou/synthesizer): performs combinatorial enumeration and concretizes a sketch sampled from the BED during inference into a Java program

## Requirements
- JDK 1.8
- Python3 (Tested with 3.5.1)
- [Tensorflow](https://www.tensorflow.org) (Tested with 1.0)
- scikit-learn (Tested with 0.18.1)

## Setup & Usage
#### Driver
```
cd /path/to/bayou/src/pl
ant
```

If you are working with the Android SDK,
```
export CLASSPATH=/path/to/android.jar
```
Bayou has an `android.jar` from Android 24 under the lib directory if needed.

After setup, run tests to ensure everything is fine:
```
cd scripts
python3 test_driver.py
```

Use the following command to run the driver on `Program.java` with the config file `config.json`:
```
java -jar /path/to/bayou/src/pl/out/artifacts/driver/driver.jar -f Program.java -c config.json [-o output.json]
```
Run driver with -h for details about the config file. The -o option can be used to output the sketch to a JSON file.

To create a single JSON file with the entire dataset, append the JSON files from each program and create a top level JSON entity called "programs" that has the entire list as the value. For example, if you have files `Program1.json`, ... `Program10000.json`, then the dataset should have the content:
```
{
  "programs": [
    <Program1.json>,
    <Program2.json>,
    ...
    <Program10000.json>
  ]
}
```

#### Model
```
cd /path/to/bayou/src/ml
export PYTHONPATH=`pwd`
```

To train LDA embeddings on keywords or types in a data file `DATA.json` generated from driver,
```
cd /path/to/bayou/src/ml/bayou/lda
python3 train.py --ntopics 30 --evidence keywords DATA.json
```
Run train.py with -h for details about the command line arguments.

To train the BED neural network on `DATA.json`,
```
cd /path/to/bayou/src/ml/bayou/core
python3 train.py --config config.json DATA.json
```
As before, run train.py with -h for details about the config file.

If you are using the trained LDA embeddings while training the BED network, copy the trained embeddings directory into the directory specified by --save (default `save`) with the name "embed_\<evidence type\>". For example, if you are using trained word embeddings for keywords, copy it to the directory `save/embed_keywords`.

#### Synthesizer
Suppose that the trained model is in a folder `trained`. Run the server to load the trained model into memory. The server will listen to a pipe (here `bayoupipe`) for inference queries:
```
mkdir server; cd server
python3 /path/to/bayou/scripts/server.py --save /path/to/trained --pipe bayoupipe
```

The synthesizer requires as input a Java class with:
- a method named `__bayou_fill` that can be empty
- arguments to this method that can be used for synthesis
- evidences towards synthesis with the method annotation `@Evidence`

See examples in `test/pl/synthesizer` for more information about the input format.

Use the provided `scripts/synthesize.sh` for running the synthesizer. First, set the environment variables `BAYOU_HOME` and `BAYOU_SERVER` (and also `bayoupipe` if you used a different name for the pipe) in this script to the home folder of bayou and where you started the server, respectively. Then, to run the synthesizer on a file `Program.java`:

```
synthesize.sh Program.java
```
If all went well, the synthesizer should output a set of Java programs with the body of the method `__bayou_fill` synthesized according to the arguments and evidences provided.

## Roadmap
- [ ] Model: Encode natural language evidence (Javadoc) better
- [ ] Model: Learn the joint distribution P(X, \theta) instead of the conditional P(X | \theta)
- [ ] Synthesizer: Extract evidence from surrounding context instead of `__bayou_fill`
- [ ] General: Gather more training data from a larger corpus 
