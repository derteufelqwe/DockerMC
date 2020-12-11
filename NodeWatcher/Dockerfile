FROM openjdk:8-jre-alpine

WORKDIR /nodewatcher
VOLUME ["/var/run/docker.sock"]

COPY target/NodeWatcher-1.0.jar /nodewatcher

CMD ["java", "-jar", "NodeWatcher-1.0.jar"]