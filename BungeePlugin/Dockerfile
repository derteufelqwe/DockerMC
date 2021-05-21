FROM dcevm11
# This name is also used by the Website
LABEL DockerMCType="BungeeCord"

STOPSIGNAL SIGINT
EXPOSE 25577
EXPOSE 8001

WORKDIR /server

HEALTHCHECK --interval=10s --start-period=60s --retries=3 --timeout=10s CMD curl -f http://localhost:8001/health || exit 101


ADD docker /server
ADD target/DMCPluginBC-1.0.jar /server/plugins/DMCPluginBC-1.0.jar


CMD ["java", "-Xmx20G", "-XX:HotswapAgent=core", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "waterfall.jar"]
