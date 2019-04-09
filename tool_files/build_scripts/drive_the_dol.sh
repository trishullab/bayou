#!/bin/bash
filename='github-java-files-train.txt'
echo Start
while read p; do
    p=${p/././java_projects}
    echo $p  
    java -jar ~/bayou/tool_files/maven_3_3_9/dom_driver/target/dom_driver-1.0-jar-with-dependencies.jar -f $p -c config.json -o output.json
done < $filename
