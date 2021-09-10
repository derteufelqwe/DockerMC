su postgres -c "pg_ctl start -D /var/lib/postgresql/data" &
/startdocker.sh &

"$@"
