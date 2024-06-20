---
title: Upgrading from 3.9 to 3.10
seotitle: Gatling upgrade from 3.9 to 3.10
description: Gatling upgrade guide from version 3.9 to 3.10.
lead: ""
date: 2023-12-14T23:00:00+02:00
lastmod: 2023-12-14T23:00:00+02:00
---

## Java 11 Baseline

Gatling now requires Java 11.
This is also true for all the satellite components such as the Maven, Gradle and sbt plugins.

## Source Compatibility

Gatling 3.10 introduces the following breaking changes:

### slf4j 2 upgrade

`slf4j` is the most popular Java logging interface, `logback` is its default implementation.

Like more and more other Java based technology, Gatling 3.10 is finally upgrading from slf4j 1 to slf4j 2, and logback 1.2 to logback 1.4.

As indicated by the major version change, slf4j 1 and slf4j 2 are not compatible.

If you're bringing into Gatling's classpath some extra libraries that are pulling slf4j or logback dependencies, you have to make sure that you end up with slf4j and logback versions that are compatible with each other:

* slf4j 1 is compatible with logback 1.2 included
* slf4j 2 is compatible with logback 1.3 or greater

With maven, you would use `mvn dependency:tree` in order to check the actual versions used and [dependencyManagement](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-management) to force the desired versions.

For more details, please check the [issue #4386](https://github.com/gatling/gatling/issues/4386)

### Feeding multiple records at once

`feed(feeder, n)` lets you feed n records at once.
Previously, this method was generating arrays of records.

As of Gatling 3.10, this method now generates Java List or Scala Seq depending on the Gatling SDK you're using.

For more details, please check the [issue #4423](https://github.com/gatling/gatling/issues/4423)

### HTTP SignatureCalculator change

`SignatureCalculator` is used to add a signature to an HTTP request before it's written on the socket.

Previously, `SignatureCalculator` was essentially a `Consumer<Request>`, which only let the user set a signature HTTP header (as HttpHeaders are mutable).

This API made it impossible to sign the request with another mean, such as adding the signature in the query or a form body.
The new API is basically `Function<Request, Request>` which makes it possible to return a new URL or a new Body.

For more details, please check the [issue #4477](https://github.com/gatling/gatling/issues/4477)

### Java SDK exitBlockOnFail signature change

The previous syntax was not consistent with the rest of the Java SDK.

{{< include-code "oldExitBlockOnFail" java >}}

must now be changed into:

{{< include-code "newExitBlockOnFail" java >}}

For more details, please check the [issue #4490](https://github.com/gatling/gatling/issues/4490)

## Binary Compatibility

Gatling 3.10 is not binary compatible with previous versions.
Any code compiled with a previous version must be recompiled in order to be executed with Gatling 3.10.

## Gradle 7.0 Baseline

As of 3.10.0, the Gatling Gradle plugin now requires Gradle 7.0.
