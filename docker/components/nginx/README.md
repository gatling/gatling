To build:

```
docker build -t gatling/nginx .
```

To run:

```
docker run -d -p 8080:80 --name gatling-nginx --link gatling-graphite:gatling-graphite-link gatling/nginx
```
