FROM python:3.6-alpine
LABEL maintainer=derteufelqwe

EXPOSE 8101
VOLUME ["/webserver/data"]
WORKDIR /webserver

ADD requirements.txt /webserver
RUN pip install -r requirements.txt

ADD Webserver.py /webserver

CMD ["python", "Webserver.py"]