FROM ubuntu:latest

ENV JDK_URL="https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases/download/dcevm-11.0.10%2B3/java11-openjdk-dcevm-linux.tar.gz"
ENV JAVA_HOME="/dcevm-11.0.10+3"
ENV PATH="$PATH:$JAVA_HOME/bin"

RUN apt update && apt install -y wget curl

RUN wget -O OpenJDK11.tar.gz $JDK_URL

RUN tar -xzvf OpenJDK11.tar.gz
