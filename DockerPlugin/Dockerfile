FROM openjdk:8-jre-alpine
# This name is also used by the Website
LABEL DockerMCType="Utility"

WORKDIR "/plugin"

COPY src/main/resources/config.json /plugin
COPY target/LogDriver-1.0.jar /plugin

CMD ["java", "-jar", "LogDriver-1.0.jar"]