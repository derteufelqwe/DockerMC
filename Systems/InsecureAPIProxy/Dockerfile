FROM nginx
LABEL MAINTAINER="derteufelqwe"

# RUN apk add --no-cache openssl
RUN apt-get update
RUN apt install -y nano iputils-ping net-tools etcd

COPY resources /script

RUN cp /script/nginx-cert.conf /etc/nginx/nginx.conf && \
    cp /script/startup.sh /startup.sh

RUN chmod +x /script/create-certs.sh /script/entrypoint.sh /startup.sh

ENV CREATE_CERTS_WITH_PW="" \
    CERTS_DIR=/data/certs \
    CERT_HOSTNAME="myserver.example.com"

ENV NODE_NAME="NO_NODE_NAME" \
    ETCDCTL_API=3 \
    ETCDCTL_ENDPOINTS="http://etcd:2379"


CMD ["./startup.sh"]