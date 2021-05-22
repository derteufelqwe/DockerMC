FROM dcevm11

# This name is also used by the Website
LABEL DockerMCType="Minecraft"

STOPSIGNAL SIGINT
EXPOSE 25565

WORKDIR /server

HEALTHCHECK --interval=10s --start-period=30s --retries=3 --timeout=5s CMD java -cp "plugins/DMCPluginMC.jar" "de.derteufelqwe.commons.PingServer" localhost 25565

COPY docker /server
COPY target/DMCPluginMC-1.0.jar /server/plugins/DMCPluginMC.jar

CMD ["java", "-Xmx20G", "-XX:HotswapAgent=core", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "papermc.jar" ]