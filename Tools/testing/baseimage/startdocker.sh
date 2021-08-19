#!/bin/bash

echo "Starting docker daemon"
dockerd --tls=false -H "tcp://0.0.0.0:2375" -H "unix:///var/run/docker.sock" > /var/log/dockerd.log 2>&1 &

# Wait until the docker daemon is started
for (( c=1; c<=10; c++ ))
  do

    if docker ps > /dev/null 2>&1 ; then
      echo "Docker daemon started"
      exit 0
    fi

    sleep 1s
  done

echo "Failed to start docker daemon"