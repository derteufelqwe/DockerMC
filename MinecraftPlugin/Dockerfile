FROM dcevm11

# This name is also used by the Website
LABEL DockerMCType="Minecraft"

STOPSIGNAL SIGINT
EXPOSE 25565
EXPOSE 8001

WORKDIR /server
VOLUME [ "/server" ]

HEALTHCHECK --interval=10s --start-period=30s --retries=2 --timeout=5s CMD java -cp "plugins/DockerMCPlugin.jar" "de.derteufelqwe.commons.PingServer" localhost 25565

COPY docker/server /server
COPY target/DockerMCPlugin.jar /server/plugins

CMD ["java", "-Xmx20G", "-XX:HotswapAgent=core", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "papermc.jar" ]