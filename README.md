# Bayou
Bayou is a data-driven program synthesis system for Java that uses learned Bayesian specifications for efficient synthesis.

Click [here](https://arxiv.org/abs/1703.05698) for the arXiv paper on Bayou.

Try Bayou on your browser! Visit www.askbayou.com.

## Requirements
- JDK 1.8
- Python3 (Tested with 3.5.1)
- [Tensorflow](https://www.tensorflow.org) (Tested with 1.2)
- scikit-learn (Tested with 0.18.1)

## Compiling and Running Bayou from Source on Ubuntu/Mac

#### 1.) Download source from GitHub:
```
git clone https://github.com/capergroup/bayou.git
```

#### 2.) Install Dependencies
```
cd bayou/tool_files/build_scripts
sudo ./install_deps_apt.sh
```
Or run ```install_deps_mac.sh``` for Mac (uses Homebrew).

#### 3.) Compile Bayou
```
./build.sh
```

#### 4.) Run Bayou
```
./start_bayou.sh &
```

Wait until you see:
```
===================================
            Bayou Ready            
===================================
```

then execute:
```
./synthesize.sh
```

You should see as output several programs separated by the characters
```
---------- BEGIN PROGRAM  ----------
```

To run Bayou on some of the provided examples, execute:
```
./synthesize.sh example_inputs/<Program.java>
```
where ```Program.java``` is the name of the example program.
