FROM debian:wheezy
MAINTAINER David Blooman <david.blooman@gmail.com>

RUN apt-get -y update
RUN apt-get -y install python \
                       wget

RUN mkdir /src

# grafana
RUN wget http://grafanarel.s3.amazonaws.com/grafana-1.9.0.tar.gz
RUN tar xzvf grafana-1.9.0.tar.gz && rm grafana-1.9.0.tar.gz
RUN mv grafana-1.9.0 /src/grafana

# config file
ADD config.js /src/grafana/config.js

# dashboards
ADD ./dashboards/annotations.json /src/grafana/app/dashboards/annotations.json
ADD ./dashboards/default.json /src/grafana/app/dashboards/default.json
ADD ./dashboards/graph-styles.json /src/grafana/app/dashboards/graph-styles.json
ADD ./dashboards/templated-graphs-nested.json /src/grafana/app/dashboards/templated-graphs-nested.json
ADD ./dashboards/templated-graphs.json /src/grafana/app/dashboards/templated-graphs.json
ADD ./dashboards/white-theme.json /src/grafana/app/dashboards/white-theme.json

WORKDIR /src/grafana

EXPOSE 8081

CMD ["python", "-m", "SimpleHTTPServer", "8081"]
