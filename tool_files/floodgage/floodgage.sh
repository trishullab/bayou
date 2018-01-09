#!/bin/bash
test -f floodgage-1.0.0.jar || (cd ../maven_3_3_9/floodgage && mvn package&& cp target/floodgage-1.0.0-jar-with-dependencies.jar ../../floodgage/floodgage-1.0.0.jar)
java -cp floodgage-1.0.0.jar:$1 edu.rice.cs.caper.floodgage.application.floodgage.FloodGageMain $2


