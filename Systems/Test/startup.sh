#!/bin/bash

# Called when the container is going to stop.
# The execution of this function should not take longer than a few seconds, otherwise Docker will just SIGKILL this container
exit_function() {
  echo "Ende"
}

trap 'true' SIGTERM

# Execute the command from CMD in the background
# Note: This will make a bash not work
"${@}" &

# Wait
wait $!

#Cleanup
exit_function
