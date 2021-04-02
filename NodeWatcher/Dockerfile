FROM openjdk:8-jre-alpine

# This name is also used by the Website
LABEL DockerMCType="Utility"

WORKDIR /nodewatcher

COPY target/NodeWatcher-1.0.jar /nodewatcher

CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5001", "-jar", "NodeWatcher-1.0.jar"]