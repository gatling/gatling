FROM ubuntu:trusty
MAINTAINER David Blooman <david.blooman@gmail.com>

RUN apt-get -y update
RUN apt-get -y install git \
                       python-django \
                       python-django-tagging \
                       python-simplejson \
                       python-memcache \
                       python-ldap \
                       python-cairo \
                       python-twisted \
                       python-pysqlite2 \
                       python-support \
                       python-pip

# graphite, carbon, and whisper
WORKDIR /usr/local/src
RUN git clone https://github.com/graphite-project/graphite-web.git
RUN git clone https://github.com/graphite-project/carbon.git
RUN git clone https://github.com/graphite-project/whisper.git
RUN cd whisper && git checkout master && python setup.py install
RUN cd carbon && git checkout 0.9.x && python setup.py install
RUN cd graphite-web && git checkout 0.9.x && python check-dependencies.py; python setup.py install

# make use of cache from gatling/carbon
RUN apt-get -y install gunicorn

RUN mkdir -p /opt/graphite/webapp
WORKDIR /opt/graphite/webapp

ENV GRAPHITE_STORAGE_DIR /opt/graphite/storage
ENV GRAPHITE_CONF_DIR /opt/graphite/conf
ENV PYTHONPATH /opt/graphite/webapp

EXPOSE 8000

CMD ["/usr/bin/gunicorn_django", "-b0.0.0.0:8000", "-w2", "graphite/settings.py"]
