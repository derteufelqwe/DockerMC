FROM consul

# Has a slightly modified entrypoint script, which adds the env variable CONSUL_NODE to set -node
# Shitty docker...

ADD entrypoint.sh /usr/local/bin/docker-entrypoint.sh

ENV CONSUL_BIND_INTERFACE="eth0"

CMD ["agent", "-dev", "-client", "0.0.0.0"]