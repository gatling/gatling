---
title: Dedicated locations installation
menutitle: Installation
seotitle: Install dedicated locations in Gatling Enterprise Cloud
description: How to install a Gatling Control Plane on Docker, to set up your Private Locations and run load generators.
lead: Run a Control Plane on Docker, to set up your Private Locations and run load generators.
date: 2021-11-15T16:00:00+00:00
lastmod: 2021-11-15T16:00:00+00:00
---

[Docker](https://www.docker.com/) is a tool for running applications in containers.

In this example:
- we use **Docker** to configure a docker service to run the Gatling Control Plane
- the [Control Plane configuration]({{< ref "../introduction#configuration" >}}) is mounted as a volume

## Control Plane service

The DockerHub repository hosts the Control Plane image, identified as [gatlingcorp/control-plane](https://hub.docker.com/r/gatlingcorp/control-plane).

The following Docker Compose file will generate a control-plane container using the latest version of the image. 

`docker-compose.yml`:
```yaml
version: '3.8'
services:
  control-plane:
    image: gatlingcorp/control-plane:latest
    container_name: control-plane
    volumes:
      - /etc/control-plane:/app/conf
```

The `/etc/control-plane` directory must include a file named `control-plane.conf`. 
This file should contain the [Control Plane configuration]({{< ref "../introduction#configuration" >}}).

Additionally, the `/etc/control-plane` directory may also include a LogBack configuration file named `logback.xml`.

{{< alert info >}}
Depending on the type of private location you choose to configure, you will need to set up appropriate credentials. 

For instance, if you are using AWS, you can mount the configuration and credentials files from your home directory by including the following lines in your `docker-compose.yml`:
```yaml
volumes:
  - /etc/control-plane:/app/conf
  - ~/.aws:/root/.aws
```
{{< /alert >}}

{{< alert info >}}
When using private packages, you will need to expose the port by including the following lines in your YAML configuration:
```yaml
ports:
  - "8082:8082"
```
{{< /alert >}}
