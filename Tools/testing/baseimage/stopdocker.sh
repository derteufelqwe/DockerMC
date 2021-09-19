echo "[Startup] Stopping docker daemon"

if pidof dockerd; then
  kill -SIGTERM $(pidof dockerd)
else
  echo "[Startup] Docker daemon not running. Removing socket file"
  rm -f /var/run/docker.pid
fi

sleep 2
