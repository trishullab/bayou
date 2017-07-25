#!/bin/bash

time echo -e '\x00''\x00''\x00''\x59' { \"apicalls\": [ \"setTitle\", \"setMessage\" ], \"types\": [ \"AlertDialog\" ], \"context\": [] } | nc 127.0.0.1 8084
