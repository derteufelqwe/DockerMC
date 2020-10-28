FROM openjdk:8-jre

EXPOSE 25577
WORKDIR /server
VOLUME ["/server", "/plugins"]

ENV TASK_NAME="NO_TASK_NAME" \
    SERVER_NAME="NO_SERVER_NAME"

HEALTHCHECK --interval=10s --start-period=10s --retries=2 --timeout=2s CMD curl -f http://localhost:8001/health || exit 1

# RUN apk --no-cache add curl bash sudo
RUN apt-get update

RUN apt install -y curl net-tools etcd

ADD docker/startup.sh /server/startup.sh
RUN chmod +x startup.sh

ADD docker/waterfall.jar /server/waterfall.jar
ADD docker/config.yml /server/config.yml
ADD target/BungeePlugin-1.0.jar /server/plugins/BungeePlugin.jar

CMD ["./startup.sh"]
#CMD ["java", "-Xmx512M", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "waterfall.jar"]