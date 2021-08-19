echo "Stopping docker daemon"
kill $(pidof dockerd)
sleep 2