To build:

```
docker build -t gatling/graphite .
```

To run:

```
docker run -d \
           --volumes-from gatling-carbon \
           --name gatling-graphite gatling/graphite
```
