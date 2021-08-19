FROM dockermctest-base

RUN mkdir /dockermc \
    && mkdir /plugin

ADD start.sh /start.sh
# Add the compiled files
ADD "build/DMCDockerPlugin.jar" "/plugin"
ADD "build/DMCServerManager.jar" "/dockermc"


### Build the BungeeCord image ###
ADD bungeeserver /tmp
ADD "build/DMCPluginBC.jar" "/tmp/bungeeserver/plugins"
ADD "build/bungeecord.dfile" "/tmp/bungeeserver/Dockerfile"

RUN /startdocker.sh \
    && docker build -t registry.swarm/dockermc-bungee:testing /tmp/bungeeserver \
    && /stopdocker.sh


### Build the Minecraft image ###
ADD minecraftserver /tmp
ADD "build/DMCPluginMC.jar" "/tmp/minecraftserver/plugins"
ADD "build/minecraft.dfile" "/tmp/minecraftserver/Dockerfile"

RUN /startdocker.sh \
    && docker build -t registry.swarm/dockermc-minecraft:testing /tmp/minecraftserver \
    && /stopdocker.sh


# Entrypoint / Cmd inherited from parent
