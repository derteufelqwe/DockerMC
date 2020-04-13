FROM openjdk:8-jre-alpine

VOLUME ["/server", "/plugins"]
WORKDIR /server

ENV WATERFALL_HOME=/server
ENV	WATERFALL_BASE_URL=https://destroystokyo.com/ci/job/Waterfall/
ENV MEMORY=512m

ADD docker/run-waterfall.sh /usr/bin/run-waterfall.sh
RUN chmod +x /usr/bin/run-waterfall.sh

RUN apk --no-cache add curl bash sudo

EXPOSE 25577

RUN set -x \
	&& addgroup -g 1000 -S waterfall \
	&& adduser -u 1000 -D -S -G waterfall waterfall \
	&& addgroup waterfall wheel

ADD docker/waterfall.jar /server/waterfall.jar
ADD docker/config.yml /server/config.yml
ADD docker/run-waterfall.sh /server/run-waterfall.sh
ADD target/artifacts/BungeePlugin.jar /server/plugins/BungeePlugin.jar

CMD ["java", "-Xmx512M", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "waterfall.jar"]