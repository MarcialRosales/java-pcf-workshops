#!/bin/bash

if [ "$#" -lt 4 ]; then 
   echo "syntax: <# requests> <concurrent req> <backend http status> <backend processing time msec>  [<front end URL>]"
   exit 1
fi
URL=${5:-localhost:8081/req1?message=hello}
ab -n $1 -c $2 -H "response: $3" -H "delay: $4" -H "hystrix: true" $URL

