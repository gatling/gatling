FROM debian:wheezy
MAINTAINER David Blooman <david.blooman@gmail.com>

RUN apt-get -y update
RUN apt-get -y install nginx-light

ADD nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["/usr/sbin/nginx"]
