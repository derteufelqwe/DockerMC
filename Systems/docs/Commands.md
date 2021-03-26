# Postgres

### Change to postgres user
``sudo -iu postgres``

### Change to postgres user and start CLI
``sudo -u postgres psql``

### Create new superuser
``createuser --superuser <name>``

### Create user with password
``psql> CREATE USER <name> WITH PASSWORD <password>``

### Change listening ip
Edit
``/etc/postgresql/12/main/postgresql.conf``
and add
``listen_addresses = '*'``

### Allow user to connect from IP
Edit
``/etc/postgresql/12/main/pg_hba.conf``
and add
``host    all             all             192.168.0.0/16          md5``


# Linux

### Show socket logs
``journalctl -u service-name.service``

### Start / stop / status of service
``sudo service <service-name> [start, stop, status]``
