FROM openjdk:8-jre

# DCEVM part
# Sources: https://github.com/dcevm/dcevm, http://hotswapagent.org/

WORKDIR "/install"

RUN apt-get update

# Download DECM pach
RUN wget -O DCEVM_latest.jar $(curl -s https://api.github.com/repos/dcevm/dcevm/releases/latest \
            | grep browser_download_url \
            | cut -d '"' -f 4 \
            )

# Download hotswap-agent.jar
RUN wget -O hotswap-agent_latest.jar "https://github.com/HotswapProjects/HotswapAgent/releases/download/RELEASE-1.4.1/hotswap-agent-1.4.1.jar"

RUN unzip DCEVM_latest.jar "linux_amd64_compiler2/product/libjvm.so"

RUN mkdir "$JAVA_HOME/lib/amd64/dcevm" \
    && cp "linux_amd64_compiler2/product/libjvm.so" "$JAVA_HOME/lib/amd64/dcevm"


# Waterfall part

STOPSIGNAL SIGINT
EXPOSE 25577
EXPOSE 8001

WORKDIR /server
VOLUME ["/server", "/plugins"]

ENV TASK_NAME="NO_TASK_NAME" \
    SERVER_NAME="NO_SERVER_NAME"

HEALTHCHECK --interval=10s --start-period=10s --retries=2 --timeout=2s CMD curl -f http://localhost:8001/health || exit 1

## RUN apk --no-cache add curl bash sudo
#RUN apt-get update
#
#RUN apt install -y curl net-tools

ADD docker/startup.sh /server/startup.sh
RUN chmod +x startup.sh

ADD docker/waterfall.jar /server/waterfall.jar
ADD docker/modules /server/modules
ADD docker/locations.yml /server/locations.yml
ADD docker/waterfall.yml /server/waterfall.yml
ADD docker/modules.yml /server/modules.yml
ADD docker/config.yml /server/config.yml
#ADD target/BungeePlugin-1.0.jar /server/plugins/BungeePlugin.jar

CMD ["java", "-Xmx20G", "-XXaltjvm=dcevm", "-javaagent:/install/hotswap-agent_latest.jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "waterfall.jar"]