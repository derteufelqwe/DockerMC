FROM dcevm11
# This name is also used by the Website
LABEL DockerMCType="BungeeCord"

STOPSIGNAL SIGINT
EXPOSE 25577

WORKDIR /server

HEALTHCHECK --interval=10s --start-period=60s --retries=3 --timeout=10s CMD java -cp "plugins/DMCPluginBC.jar" "de.derteufelqwe.commons.PingServer" localhost 25577


ADD docker /server
ADD target/DMCPluginBC-1.0.jar /server/plugins/DMCPluginBC.jar


CMD ["java", "-Xmx20G", "-XX:HotswapAgent=core", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "waterfall.jar"]
