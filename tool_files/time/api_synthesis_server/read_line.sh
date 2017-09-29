#! /bin/bash

time curl -H "Origin: http://askbayou.com" -d '{ "max program count" : "2",  "code" : "public class Test { void read(String file) {  /// call: readLine\n } }" }'  http://127.0.0.1:8080/apisynthesis
