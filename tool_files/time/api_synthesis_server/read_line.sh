#! /bin/bash

time curl -H "Origin: http://askbayou.com" -d '{ "code" : "import edu.rice.cs.caper.bayou.annotations.Evidence; public class Test { void read(String file) {  Evidence.apicalls(\"readLine\"); } }" }'  http://127.0.0.1:8080/apisynthesis
