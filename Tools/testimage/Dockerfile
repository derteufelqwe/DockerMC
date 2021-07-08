FROM nestybox/alpine-docker

### Install java ###

RUN apk update \
    && apk add openjdk8

### Install postgres ###
EXPOSE 5432

RUN apk add postgresql

RUN mkdir /run/postgresql \
    && chown postgres:postgres /run/postgresql

USER postgres

WORKDIR /var/lib/postgresql

RUN mkdir data \
    && chmod 0700 data

RUN initdb -D data \
    && echo "host all all 0.0.0.0/0 md5" >> /var/lib/postgresql/data/pg_hba.conf \
    && echo "listen_addresses='*'" >> /var/lib/postgresql/data/postgresql.conf

ADD postgresql.conf /var/lib/postgresql/data/postgresql.conf

# Configure the database
RUN pg_ctl start -D /var/lib/postgresql/data \
    && psql -c "CREATE ROLE dockermc WITH LOGIN SUPERUSER PASSWORD 'admin';" \
    && psql -c "CREATE DATABASE dockermc;" \
    && pg_ctl stop -D /var/lib/postgresql/data

USER root


### Setup docker related stuff ###
EXPOSE 2375
WORKDIR "/"
ADD start.sh start.sh

### Configure DockerMC related stuff ###



CMD ["sh", "start.sh"]
