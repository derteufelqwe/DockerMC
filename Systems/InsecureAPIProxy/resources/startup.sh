#!/bin/bash

# Called when the container is going to stop.
# The execution of this function should not take longer than a few seconds, otherwise Docker will just SIGKILL this container
exit_function() {
  echo "Unregistering container from etcd"
  etcdctl del --prefix "clients/$name"
  echo "Done."
}

trap 'true' SIGTERM

# "$(docker node inspect -f="{{ .Spec.Labels.name }}" $(docker node ls | grep "*" | awk '/\*/{print $1}'))"
# Startup command. Gets the current ip
name=$(hostname)
ip="$(ifconfig eth0 | awk '/netmask.+/{print $2}')"

echo "Registering container to etcd"
etcdctl put "clients/$name/ip" "$ip"
etcdctl put "clients/$name/name" "$NODE_NAME"

# Execute the command from CMD in the background
# Note: This will make a bash not work
"${@}" &

# Wait
wait $!

#Cleanup
exit_function
