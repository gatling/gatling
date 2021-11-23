---
title: "Maven Plugin"
description: "Maven plugin to run Gatling tests and deploy to Gatling Enterprise"
lead: "The Maven plugin allows you to run Gatling tests from the command line, without the bundle, as well as to package your simulations for Gatling Enterprise"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-11-23T09:00:00+02:00
weight: 007001
---

Using this plugin, Gatling can be launched when building your project, for example with your favorite Continuous Integration (CI) solution.
This plugin can also be used to package your Gatling project to run it on [Gatling Enterprise](https://gatling.io/enterprise/). 

## Versions

Check out available versions on [Maven Central](https://search.maven.org/search?q=g:io.gatling%20AND%20a:gatling-maven-plugin&core=gav).

Beware that milestones (M versions) are not documented for OSS users and are only released for [Gatling Enterprise](https://gatling.io/enterprise/) customers.

## Setup

### Java

In your `pom.xml`, add:

```xml
<dependencies>
  <dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>MANUALLY_REPLACE_WITH_LATEST_VERSION</version>
    <scope>test</scope>
  </dependency>
</dependencies>

<plugin>
  <groupId>io.gatling</groupId>
  <artifactId>gatling-maven-plugin</artifactId>
  <version>MANUALLY_REPLACE_WITH_LATEST_VERSION</version>
</plugin>
```

Please check our [official sample project for maven and Java](https://github.com/gatling/gatling-maven-plugin-demo-java) on GitHub.

### Kotlin

Please check our [official sample project for maven and Kotlin](https://github.com/gatling/gatling-maven-plugin-demo-kotlin) on GitHub.

### Scala

{{< alert warning >}}
Starting from version 4, this plugin no longer compiles Scala code and requires to use the `scala-maven-plugin` when using Gatling with Simulations written in Scala.
{{< /alert >}}

Please check our [official sample project for maven and Scala](https://github.com/gatling/gatling-maven-plugin-demo-scala) on GitHub.

## Configuration

The plugin supports many configuration options, eg:

```xml  
<plugin>
  <groupId>io.gatling</groupId>
  <artifactId>gatling-maven-plugin</artifactId>
  <version>MANUALLY_REPLACE_WITH_LATEST_VERSION</version>
  <configuration>
      <simulationClass>foo.Bar</simulationClass>
  </configuration>
</plugin>
```

See each goal's section below for the relevant configuration options.

## Usage

### Running your simulations

You can directly launch the `gatling-maven-plugin` with the `test` goal:

```shell
mvn gatling:test
```

Use `mvn gatling:help -Ddetail=true -Dgoal=test` to print the description of all the available configuration options on
the `test` goal.

#### Includes/Excludes filters

When running multiple simulations, you can control which simulations will be triggers with the `includes` and `excludes`
filters. Those use the ant pattern syntax and are matched against class names. Also note that those filters are only
applied against the classes that were compiled from sources in the project where the plugin is set.

```xml  
<configuration>
  <!--   ...  -->
  <runMultipleSimulations>true</runMultipleSimulations>
  <includes>
    <include>my.package.*</include>
  </includes>
  <excludes>
    <exclude>my.package.IgnoredSimulation</exclude>
  </excludes>
</configuration>
```

{{< alert tip >}}
The order of filters has no impact on execution order, simulations will be sorted by class name alphabetically.
{{< /alert >}}

### Running the Gatling Recorder

You can launch the [Gatling Recorder]({{< ref "../http/recorder" >}}):

```shell
mvn gatling:recorder
```

Use `mvn gatling:help -Ddetail=true -Dgoal=recorder` to print the description of all the available configuration options
on the `recorder` goal.

### Working with Gatling Enterprise Cloud

#### Package

You can directly package your simulations for Gatling Enterprise Cloud:

```shell
mvn gatling:enterprisePackage
```

This will generate the `target/<artifactId>-<version>-shaded.jar` package which you can then
[upload to the Cloud](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/).

#### Package and upload

You can also create and upload the package in a single command. You must already have
[configured a package](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/) (copy the package ID from
the Packages table). You will also need [an API token](https://gatling.io/docs/enterprise/cloud/reference/admin/api_tokens/)
with appropriate permissions to upload a package.

Configure the package ID (and possibly the API token, but see below for other options) on the plugin:

```xml
<plugin>
  <groupId>io.gatling</groupId>
  <artifactId>gatling-maven-plugin</artifactId>
  <version>${gatling-maven-plugin.version}</version>
  <configuration>
    <packageId>YOUR_PACKAGE_ID</packageId>
    <!-- omit apiToken when using environment variable or Java System property instead -->
    <apiToken>YOUR_API_TOKEN</apiToken>
  </configuration>
</plugin>
```

Since you probably don't want to include you secret token in your source code, you can instead configure it using either:
- the `GATLING_ENTERPRISE_API_TOKEN` environment variable
- the `gatling.enterprise.apiToken` [Java System property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)

Then package and upload your simulation to gatling Enterprise Cloud:

```shell
mvn gatling:enterpriseUpload
```

### Working with Gatling Enterprise Self-Hosted

#### Build from sources

Once you have configured the Maven plugin on your project, Gatling Enterprise Self-Hosted can build it from sources
without additional configuration.
[Add your source repository](https://gatling.io/docs/enterprise/self-hosted/reference/current/user/repositories/#sources-repository)
and configure your simulation to
[build from sources](https://gatling.io/docs/enterprise/self-hosted/reference/current/user/simulations/#option-1-build-from-sources)
using Maven.

To make sure your setup is correct, you can run the packaging command and check that you get a jar containing all the
classes and extra dependencies of your project in `target/<artifactId>-<version>-shaded.jar`:

```shell
mvn gatling:enterprisePackage
```

#### Publish to a binary repository

Alternatively, you can package your simulations and publish them to a binary repository (JFrog Artifactory, Sonatype
Nexus or AWS S3).

{{< alert tip >}}
We use the standard Maven Deploy plugin; please refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/)
for generic configuration options. Please also check the standards within your organization for the best way to configure
the credentials needed to access your binary repository.
{{< /alert >}}

Configure the `repository` and/or `snapshotRepository` block, depending on whether you want to deploy releases or snapshots.

```xml
<distributionManagement>
  <repository>
    <id>your.releases.repository.id</id>
    <url>REPLACE_WITH_YOUR_RELEASES_REPOSITORY_URL</url>
  </repository>
  <snapshotRepository>
    <id>your.snapshots.repository.id</id>
    <url>REPLACE_WITH_YOUR_SNAPSHOTS_REPOSITORY_URL</url>
  </snapshotRepository>
</distributionManagement>
```

Bind the `gatling:enterprisePackage` goal to the Maven lifecycle in the plugin configuration:

```xml
<plugin>
  <groupId>io.gatling</groupId>
  <artifactId>gatling-maven-plugin</artifactId>
  <version>${gatling-maven-plugin.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>enterprisePackage</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

The packaged artifact will be automatically attached to your project and deployed with the `shaded` classifier when you publish it:

```shell
mvn deploy
```

## Integrating with the Maven lifecycle

The plugin's goals can also be bound to the Maven lifecycle phases by configuring an execution block in the plugin configuration:

```xml
<plugin>
  <groupId>io.gatling</groupId>
  <artifactId>gatling-maven-plugin</artifactId>
  <version>MANUALLY_REPLACE_WITH_LATEST_VERSION</version>
  <executions>
    <execution>
      <goals>
        <goal>test</goal>
        <goal>enterprisePackage</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

By default:

- `test` will be bound to the `integration-test` phase, e.g. it will be triggered by `mvn integration-test` or `mvn verify`
- `enterprisePackage` will be bound to the `package` phase, e.g. it will be triggered by `mvn package`

## Overriding the logback.xml file

You can either have a `logback-test.xml` that has precedence over the embedded `logback.xml` file, or add a JVM option `-Dlogback.configurationFile=myFilePath`.

## Sources

If you're interested in contributing, you can find the [gatling-maven-plugin sources](https://github.com/gatling/gatling-maven-plugin) on GitHub.
