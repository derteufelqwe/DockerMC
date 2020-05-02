#!/bin/bash

# Environment variables
export CONSUL_PORT=${CONSUL_PORT:-8500}
export HOST_IP=${HOST_IP:-consul_server}
export CONSUL=$HOST_IP:$CONSUL_PORT
export RELOAD_INTERVAL=${RELOAD_INTERVAL:-10}
export TASK_NAME=${TASK_NAME:-"NO_TASK_NAME"}

# Important variables
ip="$(ifconfig eth0 | awk '/netmask.+/{print $2}')"

# Trap the SIGTERM to process shutdowns
trap "true" TERM

# -----  Functions  -----

# Exit function to hanle the deregistering stuff
function exit_function() {
  echo "[PROXY] Unregistering nginx from consul"
  curl --request PUT "http://${CONSUL}/v1/agent/service/deregister/${TASK_NAME}"

  echo "[PROXY] Forwarding SIGTERM to nginx"
  kill -TERM "$child"
  wait "$child"
  echo "[PROXY] Done."
}

# Load the payload.json file and replace the variables id and ip
function get_payload() {
  sed -e "s/\${id}/${TASK_NAME}/" -e "s/\${ip}/${ip}/" payload.json
}

# Start the nginx server
function start_nginx() {
  # Wait until a valid nginx config is generated. Otherwise nginx will fail to start when no bungeecord server exists
  echo "[PROXY] Generating first config for nginx..."
  until nginx -t 2>/dev/null
  do
    echo "[PROXY] No backend servers found."
    sleep "$RELOAD_INTERVAL"
  done

  echo "[PROXY] Generated first valid config. Starting nginx."
  nginx -g "daemon off;"
}

# -----  Code  -----

echo "[PROXY] Booting container. Consul host is http://$CONSUL."

# Loop until confd has updated the nginx config
until confd -onetime -backend consul -node "$CONSUL" -config-file /etc/confd/conf.d/nginx.toml 2>/dev/null
do
  echo "[PROXY] Waiting for confd to initially build nginx.conf"
  sleep 5
done

# Run confd in the background to watch the upstream servers
confd -interval "${RELOAD_INTERVAL}" -backend consul -node "$CONSUL" -config-file /etc/confd/conf.d/nginx.toml &
echo "[PROXY] Confd is listening for changes on consul..."

echo "[PROXY] Registering service ${TASK_NAME} to consul."
curl --request PUT --data "$(get_payload)" "http://${CONSUL}/v1/agent/service/register"


start_nginx &

child=$!

echo "[PROXY] Waiting for child ${child}"
wait "$child"

#Cleanup
exit_function
