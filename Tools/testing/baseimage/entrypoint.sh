/startpostgres.sh
/startdocker.sh


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

/stopdocker.sh
/stoppostgres.sh
