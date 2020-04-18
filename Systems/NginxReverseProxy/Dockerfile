FROM nginx
LABEL developer=derteufelqwe

EXPOSE 25577
WORKDIR "/server"

ADD https://github.com/kelseyhightower/confd/releases/download/v0.16.0/confd-0.16.0-linux-amd64  /usr/local/bin/confd

RUN chmod +x /usr/local/bin/confd \
    && mkdir -p /etc/confd/{conf.d,templates}

COPY startup.sh startup.sh
COPY nginx.tmpl /etc/confd/conf.d/nginx.tmpl
COPY nginx.toml /etc/confd/templates/nginx.toml


ENTRYPOINT ["./startup.sh"]

CMD ["nginx", "-g", "daemon off;"]