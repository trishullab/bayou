# DOM Driver
Use this module to generate data from your own corpus of Java programs.

### Install Dependencies
```
cd bayou/tool_files/build_scripts
sudo ./install_dependencies.sh
```

### Compile the DOM Driver
```
cd bayou/tool_files/maven_3_3_9/dom_driver
mvn package
```

### Run the DOM Driver
The Driver needs two files to run: a configuration JSON file, and a .java file containing the source code of a class. The configuration file allows you to specify what APIs or classes to extract data on, maximum number of sequences from each program, etc.

To learn more about the configuration options:
```
java -jar target/dom_driver-1.0-jar-with-dependencies.jar
```

To extract data from a file `Test.java` with the configuration file `config.json` and store the output in `output.json`:
```
java -jar target/dom_driver-1.0-jar-with-dependencies.jar -f Test.java -c config.json -o output.json
```
