FROM debian:wheezy
MAINTAINER David Blooman <david.blooman@gmail.com>

RUN apt-get -y update
RUN apt-get -y install openjdk-7-jre \
                       wget

# elasticsearch
RUN wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.0.1.deb
RUN dpkg -i elasticsearch-1.0.1.deb && rm elasticsearch-1.0.1.deb

# disable dynamic scripting
RUN echo 'script.disable_dynamic: true' >> /etc/elasticsearch/elasticsearch.yml

EXPOSE 9200

CMD ["/usr/share/elasticsearch/bin/elasticsearch"]
