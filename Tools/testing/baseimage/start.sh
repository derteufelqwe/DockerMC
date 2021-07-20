su postgres -c "pg_ctl start -D /var/lib/postgresql/data"
dockerd --tls=false -H "tcp://0.0.0.0:2375" -H "unix:///var/run/docker.sock" > /var/log/dockerd.log 2>&1 &
"/bin/sh"
