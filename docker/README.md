Gatling Graphite Charts
=========

This is a generic metrics and logging tool for Load testing.  It uses [Graphite](http://graphite.readthedocs.org/en/latest/) and [Grafana](http://grafana.org/) for the front end, with [Carbon](http://graphite.wikidot.com/carbon) as the backend.  Nginx is used as a reverse proxy and is in it's own container.  [Elasticsearch](http://www.elasticsearch.org/) is used as the JSON store for the Grafana dashboards, this will mean that you don't need to restart the service when you want to add new data and dashboards.

To build graphite-charts:

```
./build
```

To start:

```
./start
```

To stop:

```
./stop
```
