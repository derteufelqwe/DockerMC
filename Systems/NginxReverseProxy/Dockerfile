FROM nginx
LABEL developer=derteufelqwe

EXPOSE 25577
WORKDIR "/server"

ENV TASK_NAME "NO_TASK_NAME"

RUN apt-get update
RUN apt install -y curl net-tools

ADD https://github.com/kelseyhightower/confd/releases/download/v0.16.0/confd-0.16.0-linux-amd64  /usr/local/bin/confd

RUN chmod +x /usr/local/bin/confd \
    && mkdir -p /etc/confd/{conf.d,templates}

COPY nginx.toml /etc/confd/conf.d/nginx.toml
COPY nginx.tmpl /etc/confd/templates/nginx.tmpl
COPY payload.json payload.json
COPY startup.sh startup.sh

CMD ["./startup.sh"]