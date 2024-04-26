---
title: Upgrading from 3.10 to 3.11
seotitle: Gatling upgrade from 3.10 to 3.11
description: Gatling upgrade guide from version 3.10 to 3.11.
lead: ""
date: 2024-04-08T23:00:00+02:00
lastmod: 2024-04-08T23:00:00+02:00
---

## Dropping the old `${}` Gatling Expression Language

In the old days, Gatling used to use a `${}` syntax to define expressions for its Expression Language.
This pattern caused clashes with the String interpolation features that were introduced In Scala, then Kotlin and finally Java.
To avoid confusion and clashes, Gatling 3.7.0, released in November 2021, introduced a new `#{}` syntax and deprecated the old one.
Keeping on using the old syntax would still work but would issue some WARN logs.

{{< alert warning >}}
Gatling 3.11.0 is removing the old syntax, meaning you must now use `#{}` and that `${}` will no longer be interpreted as a Gatling Expression.
{{< /alert >}}

## Dropping old names for methods that were renamed a long time ago

<table>
    <thead>
        <tr>
            <td>Gatling SDK</td>
            <td>Module</td>
            <td>Old method name</td>
            <td>New method name</td>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Java</td>
            <td>Core: switches</td>
            <td>`Choice#withKey`</td>
            <td>`onCase`</td>
        </tr>
        <tr>
            <td>Java</td>
            <td>Core: switches</td>
            <td>`Choice#withWeight`</td>
            <td>`percent`</td>
        </tr>
        <tr>
            <td>Java/scala</td>
            <td>Core: feeders</td>
            <td>`convert`</td>
            <td>`transform`</td>
        </tr>
        <tr>
            <td>Scala</td>
            <td>Core: open injection profile</td>
            <td>`heavisideUsers`</td>
            <td>`stressPeakUsers`</td>
        </tr>
        <tr>
            <td>Java/Scala</td>
            <td>HTTP: resources inferring</td>
            <td>`WhiteList`</td>
            <td>`AllowList`</td>
        </tr>
        <tr>
            <td>Java/Scala</td>
            <td>HTTP: resources inferring</td>
            <td>`BlackList`</td>
            <td>`DenyList`</td>
        </tr>
        <tr>
            <td>Java/Scala</td>
            <td>HTTP: polling</td>
            <td>`polling`</td>
            <td>`poll`</td>
        </tr>
        <tr>
            <td>Java/Scala</td>
            <td>HTTP: HTTP/1.1 resources</td>
            <td>`maxConnectionsPerHostLikeXXX`</td>
            <td>`maxConnectionsPerHost(n)`</td>
        </tr>
        <tr>
            <td>Java/Scala</td>
            <td>HTTP: checks</td>
            <td>`ignoreDefaultChecks`</td>
            <td>`ignoreProtocolChecks`</td>
        </tr>
        <tr>
            <td>Java/Scala</td>
            <td>MQTT: checks</td>
            <td>`wait`</td>
            <td>`await`</td>
        </tr>
    </tbody>
</table>

## Dropping unused features

The HTTP protocol `virtualHost` method has been dropped.
If you want to hit a specific IP address while forcing the hostname, you can achieve the same result with bypassing the DNS resolution using the existing `hostNameAliases`.

## Replacing the hand-made bundle with a maven wrapper based one

The Gatling standalone bundle was created as a means for non developers to start using Gatling without having to install anything but a JDK on their machine.

Over the years, we've realized several limitations:
1. it's not compatible with IDEs such as IntelliJ IDEA, eclipse or VSCode as it has a non-standard structure
2. it doesn't let the user grow skills and become familiar with professional usage such as IDEs and build tools
3. it requires a huge effort to maintain a Scala incremental compiler integration

As a result, we've opted for providing a maven wrapper based bundle.
This bundle ships all the necessary libraries and works offline, both for Gatling and for maven itself.
This way, it can be used by users who are not familiar with configuring maven to work in their Corporate network.
As it uses a maven project layout, it can be easily imported in any IDE.

### Migrating from the standalone bundle to the Maven-based bundle

The following procedure assists you to migrate from the standalone bundle to the new Maven-based bundle.

1. Download the [Maven-based bundle](https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/{{< var gatlingVersion >}}/gatling-charts-highcharts-bundle-{{< var gatlingVersion >}}.zip). 
2. Upgrade your existing project to [Gatling 3.10.5]({{< ref "/release-notes/oss/upgrading/3.9-to-3.10/" >}}) if you are not already on this version.
3. From your existing project: 
  - Copy all files from `user-files/simulations/` of the Gatling bundle to `src/test/java/` in your Maven project.
  - Copy all files from `user-files/resources/` of the Gatling bundle to `src/test/resources/` in your Maven project.
  - (optional) Open `pom.xml` in the Maven project and modify it to compile with your version of Java (example for Java 11):

  ```xml
  <maven.compiler.release>11</maven.compiler.release>
  ```
4. Verify a successful migration by starting a simulation:
  {{< code-toggle console >}}
  Linux/MacOS: ./mvnw gatling:test
  Windows: mvnw.cmd gatling:test
  {{</ code-toggle >}}

## Dropping relative filesystem path resolution for resources (feeders, bodies)

Before Gatling 3.11, it was possible to define feeder and body files paths as relative to the current location, typically the root of the project.
This way of doing things was very error-prone.

As of Gatling 3.11, we only support:
* absolute paths
* classpath paths, meaning that a file in `src/test/resources/data/foo.csv` must be referenced as `data/foo.csv`

## Upgrading the gatling-maven-plugin to 4.8.0

The Maven plugin now requires at least 3.6.3.
This is the same baseline as the latest versions of the main Maven plugins.

{{< alert warning >}}
This new version introduces a major behavior change.
{{< /alert >}}

* by default, it runs in interactive mode and lets you select the desired simulation amongst the list of available ones
* when running on a CI, it automatically switches to non-interactive mode and fails if no simulation is explicitly specified

This new behavior is more aligned with local usage from a human and CI usage from a program.

## Upgrading the gatling-gradle-plugin to 3.11.0

{{< alert warning >}}
This new version is a major overhaul and introduces several breaking changes.
{{< /alert >}}

* It has the same behavior as the new version of the Maven plugin: interactive by default, unless when running on a CI.
* It now uses a `--simulation=<FullyQualifiedClassName>` option to force the desired simulation instead of the non-standard `gatlingRun-<FullyQualifiedClassName>` pattern.
* It no longer applies the `scala` plugin automatically. It's the responsibility of the user to apply it if needed. See the [demo project](https://github.com/gatling/gatling-gradle-plugin-demo-scala/blob/main/build.gradle#L2) for how to apply the Scala plugin in your gradle build.
* It no longer generate a logback conf when there's none defined. `logLevel` and `logHttp` have been dropped and must be removed from your `build.gradle`. Instead, you must add a logback conf file as demoed in our [demo projects](https://github.com/gatling/gatling-gradle-plugin-demo-java/tree/main/src/gatling/resources).
