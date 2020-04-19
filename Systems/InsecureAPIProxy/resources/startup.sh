#!/bin/bash

# Called when the container is going to stop.
# The execution of this function should not take longer than a few seconds, otherwise Docker will just SIGKILL this container
exit_function() {
  echo "Unregistering api-proxy container from etcd"
  etcdctl del --prefix "clients/$name" || exit 101
  echo "Done."
}

trap 'true' SIGTERM


# Startup command. Gets the current ip
name=$(hostname)
ip="$(ifconfig eth0 | awk '/netmask.+/{print $2}')"

echo "Registering api-proxy container to etcd"
etcdctl put "clients/$name/ip" "$ip" || exit 101
etcdctl put "clients/$name/name" "$NODE_NAME" || exit 101

echo "Starting API-Proxy"
nginx -g "daemon off;"

#Cleanup
exit_function
