FROM dockermctest-base

RUN mkdir /dockermc \
    && mkdir /plugin

ADD start.sh /start.sh
# Add the compiled files
ADD "build/DMCDockerPlugin.jar" "/plugin"
ADD "build/DMCServerManager.jar" "/dockermc"
