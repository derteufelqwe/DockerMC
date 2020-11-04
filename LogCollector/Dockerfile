FROM openjdk:8-jre-alpine

WORKDIR /logcollector
VOLUME ["/var/run/docker.sock"]

COPY target/LogCollector-1.0.jar /logcollector

CMD ["java", "-jar", "LogCollector-1.0.jar"]