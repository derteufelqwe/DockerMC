echo "[Startup] Starting postgresql server"
su postgres -c "pg_ctl start -D /var/lib/postgresql/data"
