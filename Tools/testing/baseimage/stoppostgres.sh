echo "[Startup] Stopping postgresql server"
su postgres -c "pg_ctl stop -D /var/lib/postgresql/data"
