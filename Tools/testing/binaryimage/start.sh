su postgres -c "pg_ctl start -D /var/lib/postgresql/data"
/startdocker.sh

# Configure and start the Docker plugin
export DB_PASSWORD="admin"
java -jar "/plugin/DMCDockerPlugin.jar" &

"$@"
