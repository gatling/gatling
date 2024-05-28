---
title: Package Generation
seotitle: Generate packages for Gatling Enterprise Self-Hosted
description: Learn how to package Gatling simulations for Gatling Enterprise Self-Hosted from the Gatling zip bundle, or from a Maven, sbt, or Gradle project.
aliases:
  - binary
date: 2021-03-26T18:06:39+01:00
lastmod: 2024-03-26T09:00:00+02:00
---

## Gatling Enterprise Gatling Versions

Gatling Enterprise actually uses custom versions of the Gatling components. Those binaries are not open source and their usage is restricted to Gatling Enterprise.

When you deploy simulations with Gatling Enterprise, it replaces your Gatling OSS dependencies with their custom counterparts.

## Configuring Gatling Projects

You can configure your project to allow Gatling Enterprise to build it from the source code, or you can decide to publish
a binary package yourself and let Gatling Enterprise retrieve it.

Either way, you will need to configure a project using one of the supported build tools, with the corresponding Gatling
plugin. To set up you project, please refer to the documentation pages of the respective plugins:

- [Gatling plugin for Maven]({{< ref "../../../integrations/build-tools/maven-plugin" >}}) (for Java, Kotlin and Scala)
- [Gatling plugin for Gradle]({{< ref "../../../integrations/build-tools/gradle-plugin" >}}) (for Java, Kotlin and Scala)
- [Gatling plugin for sbt]({{< ref "../../../integrations/build-tools/sbt-plugin" >}}) (for Scala)

Once your project is ready, you can then [configure a repository]({{< ref "repositories" >}}) and
[a simulation]({{< ref "simulations" >}}).

{{< alert tip >}}
Please also check the dedicated pages of the build plugins if you intend to pre-generate the packages and deploy them in a maven repository.
{{< /alert >}}

## Note on Feeders

A typical mistake with Gatling and Gatling Enterprise is to rely on having an exploded maven/gradle/sbt project structure and try loading files from the project filesystem.

This filesystem structure will be gone once Gatling Enterprise will have compiled your project and uploaded your binaries on the injectors.

If your feeder files are packaged with your test sources, you must resolve them from the classpath.
This way will always work, both locally and with Gatling Enterprise.

```scala
// incorrect
val feeder = csv("src/test/resources/foo.csv")

// correct
val feeder = csv("foo.csv")
```

## Specific Gatling Features

### Load Sharding

Injection rates and throttling rates are automatically distributed amongst nodes.

However, Feeders data is not automatically sharded, as it might not be the desired behavior.

If you want data to be unique cluster-wide, you have to explicitly tell Gatling to shard the data, e.g.:

```scala
val feeder = csv("foo.csv").shard
```

Assuming a CSV file contains 1000 entries, and 3 Gatling nodes, the entries will be distributed the following way:

- First node will access the first 333 entries
- Second node will access the next 333 entries
- Third node will access the last 334 entries

{{< alert tip >}}
`shard` is available in Gatling OSS DSL but is a noop there. It's only effective when running tests with Gatling Enterprise.
{{< /alert >}}

## Resolving Injector Location in Simulation

When running a distributed test from multiple locations, you could be interested in knowing where a given injector is deployed in order to trigger specific behaviors depending on location.

For example, you might want to hit `https://mydomain.co.uk` `baseUrl` if injector is deployed on AWS London, and `https://mydomain.com` otherwise.

You can resolve in your simulation code the name of the pool a given injector is deployed on:

```scala
val poolName = System.getProperty("gatling.enterprise.poolName")
val baseUrl = if (poolName == "London") "https://domain.co.uk" else "https://domain.com"
```

{{< alert tip >}}
This System property is only defined when deploying with Gatling Enterprise.
It's not defined when running locally with any Gatling OSS launcher.
{{< /alert >}}
