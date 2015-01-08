To build:

```
docker build -t gatling/elasticsearch .
```

To run:

```
docker run -d \
           -p 9200:9200 \
           --name gatling-elasticsearch gatling/elasticsearch
```
