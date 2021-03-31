FROM dcevm11
# This name is also used by the Website
LABEL DockerMCType="BungeeCord"

STOPSIGNAL SIGINT
EXPOSE 25577
EXPOSE 8001

WORKDIR /server
VOLUME ["/server", "/plugins"]


HEALTHCHECK --interval=10s --start-period=60s --retries=3 --timeout=10s CMD curl -f http://localhost:8001/health || exit 101

ADD docker/startup.sh /server/startup.sh
RUN chmod +x startup.sh

ADD docker/waterfall.jar /server/waterfall.jar
ADD docker/modules /server/modules
ADD docker/locations.yml /server/locations.yml
ADD docker/waterfall.yml /server/waterfall.yml
ADD docker/modules.yml /server/modules.yml
ADD docker/config.yml /server/config.yml
ADD target/BungeePlugin-1.0.jar /server/plugins/BungeePlugin-1.0.jar


CMD ["java", "-Xmx20G", "-XX:HotswapAgent=core", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "waterfall.jar"]
