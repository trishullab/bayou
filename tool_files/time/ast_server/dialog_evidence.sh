#!/bin/bash

time curl -H "Content-Type: application/json"  -d '{ "request type" : "generate asts", "max ast count" : 1  , "evidence": "{\"apicalls\": [ \"setTitle\", \"setMessage\" ], \"types\": [ \"AlertDialog\" ], \"context\": []}" }' http://localhost:8084/
