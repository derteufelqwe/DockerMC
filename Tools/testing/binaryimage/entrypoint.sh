/startpostgres.sh
/startdocker.sh

# Configure and start the Docker plugin
export DB_PASSWORD="admin"
java -jar "/plugin/DMCDockerPlugin.jar" &


# Trap and forward the stop signal
function sigterm_hook() {
  kill -TERM "$child"
  wait "$child"
}
trap sigterm_hook SIGTERM SIGINT SIGQUIT


# Execute the command in a background shell, then wait for it to complete.
# This is required so all signals can be forwarded to the child process
echo "[Startup] Executing CMD..."
"$@" &

child=$!
wait $child
echo "[Startup] CMD done."


pid=$(pgrep -f DMCDockerPlugin.jar)
echo "[Startup] Stopping DMCDockerPlugin with PID $pid"
kill "$pid"

/stopdocker.sh
/stoppostgres.sh
