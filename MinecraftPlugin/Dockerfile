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


# PaperMC part

STOPSIGNAL SIGINT
EXPOSE 25565
EXPOSE 8001

WORKDIR /server
VOLUME [ "/server" ]

ENV TASK_NAME "NO_TASK_NAME"

HEALTHCHECK --interval=10s --start-period=10s --retries=2 --timeout=2s CMD curl -f http://localhost:8001/health || exit 1

COPY docker/server /server
COPY target/MinecraftPlugin-1.0.jar /server/plugins

CMD ["java", "-Xmx20G", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "papermc.jar" ]