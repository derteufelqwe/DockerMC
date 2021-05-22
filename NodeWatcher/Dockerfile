FROM openjdk:8-jre-alpine

# This name is also used by the Website
LABEL DockerMCType="Utility"

WORKDIR /nodewatcher

#HEALTHCHECK --interval=10s --start-period=20s --retries=3 --timeout=10s CMD curl -f http://localhost:8001/health || exit 101

COPY target/DMCNodeWatcher-1.0.jar /nodewatcher/DMCNodeWatcher.jar

CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5001", "-jar", "DMCNodeWatcher.jar"]
