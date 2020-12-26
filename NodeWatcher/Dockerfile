FROM openjdk:8-jre-alpine

# This name is also used by the Website
LABEL DockerMCType="Utility"

WORKDIR /nodewatcher
VOLUME ["/var/run/docker.sock"]

COPY target/NodeWatcher-1.0.jar /nodewatcher

CMD ["java", "-jar", "NodeWatcher-1.0.jar"]