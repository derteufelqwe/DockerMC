FROM openjdk:8-jre-alpine

RUN apk add curl

WORKDIR /server
EXPOSE 25565
EXPOSE 8001
VOLUME [ "/server" ]

ENV TASK_NAME "NO_TASK_NAME"

HEALTHCHECK --interval=10s --start-period=10s --retries=2 --timeout=2s CMD curl -f http://localhost:8001/health || exit 1

COPY docker/server /server
COPY target/artifacts/MinecraftPlugin.jar /server/plugins

CMD [ "java", "-Xmx512M", "-jar", "papermc.jar" ]