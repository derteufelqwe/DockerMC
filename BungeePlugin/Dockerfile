FROM openjdk:8-jre

EXPOSE 25577
WORKDIR /server
VOLUME ["/server", "/plugins"]

ENV NODE_NAME="NO_NODE_NAME" \
    ETCDCTL_API=3 \
    ETCDCTL_ENDPOINTS="http://etcd:2379"

# RUN apk --no-cache add curl bash sudo
RUN apt-get update

RUN apt install -y curl net-tools etcd

ADD docker/waterfall.jar /server/waterfall.jar
ADD docker/config.yml /server/config.yml
ADD docker/run-waterfall.sh /server/run-waterfall.sh
ADD target/artifacts/BungeePlugin.jar /server/plugins/BungeePlugin.jar
ADD docker/startup.sh /server/startup.sh

RUN chmod +x startup.sh

CMD ["./startup.sh"]