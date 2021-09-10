FROM nestybox/alpine-docker


### Install dependencies ###
RUN apk update \
    && apk add openjdk8 \
    && apk add bash \
    && apk add maven


### Install postgresql ###
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


### Setup docker control ###
EXPOSE 2375
WORKDIR "/"

ADD start.sh start.sh
ADD startdocker.sh /
ADD stopdocker.sh /

RUN chmod +x /startdocker.sh \
    && chmod +x /stopdocker.sh


### Setup docker ###
RUN /startdocker.sh \
    && docker pull registry:latest \
    && docker pull redis:latest \
    && docker pull frapsoft/openssl:latest \
    && docker pull xmartlabs/htpasswd:latest \
    && docker pull openjdk:8-jre-alpine \
    && /stopdocker.sh

RUN /startdocker.sh \
    && docker swarm init \
    && /stopdocker.sh


### Configure DockerMC related stuff ###


### Run configuration ###
ENTRYPOINT ["sh", "start.sh"]

CMD ["sleep", "infinity"]
