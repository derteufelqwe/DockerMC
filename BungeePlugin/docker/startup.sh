#!/bin/bash

# DEPRECATED

export PORT=${PORT:-25577}

# Called when the container is going to stop.
# The execution of this function should not take longer than a few seconds, otherwise Docker will just SIGKILL this container
exit_function() {
  echo "Forwarding SIGTERM to server"
  kill -TERM "$child"
  wait "$child"
  echo "Done."
}

trap 'true' SIGTERM


echo "Starting BungeeCord..."
java -Xmx20G -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar waterfall.jar &

child=$!

wait "$child"

#Cleanup
exit_function