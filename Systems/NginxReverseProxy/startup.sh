#!/bin/bash

export ETCD_PORT=${ETCD_PORT:-2379}
export HOST_IP=${HOST_IP:-http://etcd}
export ETCD=$HOST_IP:$ETCD_PORT
export RELOAD_INTERVAL=${RELOAD_INTERVAL:-10}

echo "[Confd] Booting container. ETCD host: $ETCD"

# Loop until confd has updated the nginx config
until confd -onetime -backend etcdv3 -node $ETCD -config-file /etc/confd/conf.d/nginx.toml; do
  echo "[Confd] Waiting for confd to initially build nginx.conf"
  sleep 5
done

# Run confd in the background to watch the upstream servers
confd -interval ${RELOAD_INTERVAL} -backend etcdv3 -node $ETCD -config-file /etc/confd/conf.d/nginx.toml &
echo "[Confd] Confd is listening for changes on etcd..."

# Wait until a valid nginx config is generated. Otherwise nginx will fail to start when no bungeecord server exists
echo "[nginx] Starting nginx..."
until nginx -t
do
  sleep $RELOAD_INTERVAL
done

nginx -g "daemon off;"

echo "Ende"
