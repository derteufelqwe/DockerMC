FROM ubuntu:trusty
LABEL version=14

COPY startup.sh startup.sh

RUN chmod +x startup.sh

ENTRYPOINT ["/startup.sh"]

CMD ["/bin/bash"]