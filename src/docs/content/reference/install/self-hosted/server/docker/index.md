---
title: Installation with Docker
seotitle: Docker installation guide for Gatling Enterprise Self-Hosted
description: Learn how to install Gatling Enterprise with Docker.
lead: Learn how to install Gatling Enterprise with Docker.
date: 2021-03-26T17:31:42+01:00
lastmod: 2023-03-07T16:42:03+00:00
---

Running Gatling Enterprise with [Docker](https://docs.docker.com/engine/) is the recommended solution to get started quickly, as it requires the minimal amount of setup and is the easiest way to upgrade to newer versions.

We provide a [Docker Compose](https://docs.docker.com/compose/) configuration as an example. While it is a good start, it is not recommended for production workload as it requires having the database and Gatling Enterprise in dedicated hosts for performance reasons.

You can reuse every configuration shown here to spawn your own Docker based environment, could it be volumes that needs to be backed up, properties, ports mapping, and so on.

## Quick Checklist

1. Contact support with your Docker Hub username to get access to the Gatling Enterprise Docker image
2. Prepare a folder to hold the Docker Compose configuration, and inside this folder, make three subfolders:

  - `cassandra-data`, kept empty
  - `frontline-conf` prepared with the default [`frontline.conf`]({{< ref "configuration#default-configuration-file" >}}) and [`logback.xml`]({{< ref "#logging" >}})
  - `frontline-keys`, kept empty

3. Optionally, create this subfolder:

  - `repositories-cache`, kept empty

You can skip to the [Docker Compose configuration section]({{< ref "#configuration" >}}) if you already know the details.

## Detailed Checklist

Create a folder that will hold the Docker Compose configuration.
We will make multiple subfolders inside to hold the required volumes.
These volumes are meant to be backed up and reused when upgrading to one version to the next:

* `cassandra-data` holds all the data of Gatling Enterprise
* `frontline-conf` contains the core configuration of Gatling Enterprise
* `frontline-keys` contains secrets that are used to either spawn injectors or get access to private sources used to run tests

Optionally, create this subfolder:

* `repositories-cache` contains the cache of all the build tools that can be used with Gatling Enterprise, such as: Maven, Gradle and sbt

You can create all three (or four) folders inside the folder of your choice.
Later on, we will add the main Docker Compose configuration into this main folder.

### Getting Gatling Enterprise's Docker image

Gatling Enterprise image is hosted as a private image on our organization's [Docker Hub](https://hub.docker.com/r/gatlingcorp/frontline).
Please contact our support and provide us with your Docker Hub username so we can grant you access.

{{< alert warning >}}
This access is only given to Gatling Enterprise Self-Hosted customers.
{{< /alert >}}

Gatling Enterprise will be available under the tag name `gatlingcorp/frontline:{{< var selfHostedVersion >}}`.

You can pull it directly to validate your access:

```console
docker pull gatlingcorp/frontline:{{< var selfHostedVersion >}}
```

{{< alert tip >}}
Make sure you are logged in using `docker login` with the username you provided to our support.
{{< /alert >}}

### Preparing the Cassandra database

We will be using the official Cassandra image under the tag name `cassandra:4`.

This image will push data into a folder named `cassandra-data`, that is mapped to the container's inner folder `/var/lib/cassandra`.

Cassandra is available on port `9042`. It won't be visible in the configuration as we will put all services in the same network.

### Copy Gatling Enterprise default configuration

If not already done, add a folder called `frontline-conf` into the main Docker Compose folder.
It will store the license information and the key used for data encryption.

In this folder, copy the [Gatling Enterprise default configuration]({{< ref "configuration#default-configuration-file" >}}) and name it `frontline.conf`.

{{< alert tip >}}
It is possible to override some configuration using environment variables for convenience, such as `FRONTLINE_CASSANDRA_HOST` and `FRONTLINE_CASSANDRA_PORT`.

See [full configuration below]({{< ref "#configuration" >}}).
{{< /alert >}}

### Copy logging configuration {#logging}

The default log behavior is too verbose from a performance point of view.
You will have to put this `logback.xml` file in the same directory as the previous `frontline.conf` file.

Inside `frontline-conf`, add `logback.xml`:

{{< include-code "logback-sample.xml" xml >}}

Notice the `<logger>` line which put the log level of Gatling Enterprise to `INFO`.
When contacting support about anything, it is useful to put this specific log level `DEBUG` in order to provide us with as much debugging information as possible.

### Prepare private keys folder

Finally, we need to prepare a folder called `frontline-keys` that will hold uploaded private keys. It is required by features like Git cloning and by most cloud providers.

You can leave it empty.

### Prepare repositories cache folder

This step is optional.

Building a Gatling project for the first time, using Maven, Gradle, or sbt, will download some dependencies that will be stored on disk. It is possible to make a volume of these folders to avoid downloading the same files again each time you spin up a new Gatling Enterprise container.

Prepare a folder called `repositories-cache` that will hold these downloaded dependencies.

You can leave it empty.

## Assembling and using Docker Compose {#configuration}

Put this configuration into a file called `docker-compose.yml` and after customizing the host folders you configured previously, then run:

```console
docker-compose up -d
```

{{< alert tip >}}
You should change your `frontline.conf` to let Gatling Enterprise know where to find cassandra. Because of the docker context, they are two different containers and cannot, by default, reach each other as localhost. If really they are on the same host, you can add `--network host` argument to docker run.
{{< /alert >}}

{{< alert tip >}}
Depending on your needs, you may need to configure additional volumes on the Gatling Enterprise container (SSL certificate if HTTPS is configured, or keystore/truststore for LDAP support)
{{< /alert >}}

## Docker compose

For your convenience, here are some docker compose instructions to set up a quick test environment. This is enough to start Gatling Enterprise locally.

**Cassandra container:**

Based on cassandra image `cassandra:4.1`, you'll have to bind a local directory on the volume to store and keep your data. A healthcheck will be started on the container.

**Gatling Enterprise container:**

Wait until cassandra container healthcheck indicates that all is well.
Set the version of Gatling Enterprise you want to deploy, bind a local directory for Gatling Enterprise configuration and keys.

### Prerequisite

Create a folder to store the files, add the given `docker-compose.yml`, then run `docker-compose up -d`.
Once ready, visit Gatling Enterprise at [http://localhost:10542](http://localhost:10542)!

```yaml
version: '2.4'
services:
  cassandra:
    container_name: cassandra
    image: cassandra:4.1
    environment:
      - CASSANDRA_CLUSTER_NAME=FrontLine
    volumes:
      - ./cassandra-data:/var/lib/cassandra
      # - <path to your cassandra directory (default empty)>:/var/lib/cassandra
    networks:
      - frontline-network
    healthcheck:
      test: ["CMD-SHELL", "[ $$(nodetool statusgossip) = running ]"]
      interval: 30s
      timeout: 10s
      retries: 10
  frontline:
    container_name: frontline
    image: gatlingcorp/frontline:{{< var selfHostedVersion >}}
    ports:
      - 10542:10542
    networks:
      - frontline-network
    environment:
      # FRONTLINE_CASSANDRA_HOST and FRONTLINE_CASSANDRA_PORT are used to update frontline.conf
      - FRONTLINE_CASSANDRA_HOST=cassandra
      # Provides a default pool that can be used for testing. Not recommended for production.
      - FRONTLINE_ENABLE_LOCAL_POOL=true
    volumes:
      - ./frontline-conf:/opt/frontline/conf
      # - <path to your frontline conf directory (default contains frontline.conf)>:/opt/frontline/conf
      - ./frontline-keys:/opt/frontline/keys
      # - <path to your frontline keys directory (default empty)>:/opt/frontline/keys
      # If using Maven
      - ./repositories-cache/maven:/opt/frontline/.m2
      # If using Gradle
      - ./repositories-cache/gradle:/opt/frontline/.gradle
      # If using sbt
      - ./repositories-cache/ivy2:/opt/frontline/.ivy2
      - ./repositories-cache/sbt:/opt/frontline/.sbt
    depends_on:
      cassandra:
        condition: service_healthy
networks:
  frontline-network:
    driver: bridge
```

{{< alert tip >}}
Depending on your needs, you may need to configure additional volumes on the Gatling Enterprise container (SSL certificate if HTTPS is configured, or keystore/truststore for LDAP support)
{{< /alert >}}
