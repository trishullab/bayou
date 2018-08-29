# Batch DOM Driver
Use this module to generate data from your own corpus of Java programs. This is a multi-threaded version of the [DOM driver](https://github.com/capergroup/bayou/tree/master/tool_files/maven_3_3_9/dom_driver).

### Install Dependencies
```
cd bayou/tool_files/build_scripts
sudo ./install_dependencies.sh
```

### Compile the Batch DOM Driver
```
cd bayou/tool_files/maven_3_3_9/batch_dom_driver
mvn package
```

### Run the Batch DOM Driver
The Driver needs two files to run: a configuration JSON file, and a file that contains a _list_ of .java files (full path, one per line). Check out the DOM driver page for details about the configuration file.

To extract data from all files in the list `files.txt` with the configuration file `config.json`:
```
java -jar target/batch_dom_driver-1.0-jar-with-dependencies.jar files.txt config.json
```
The data extracted from each .java file will be stored in the same directory with the added extension .java.json.
