---
title: "Maven Plugin"
description: "How to use the maven plugin for Gatling to run tests and deploy them to Gatling Enterprise."
lead: "The Maven plugin allows you to run Gatling tests from the command line, without the bundle, as well as to package your simulations for Gatling Enterprise"
date: 2021-04-20T18:30:56+02:00
lastmod: 2023-03-09T17:00:00+00:00
weight: 2080100
---

Using this plugin, Gatling can be launched when building your project, for example with your favorite Continuous Integration (CI) solution.
This plugin can also be used to package your Gatling project to run it on [Gatling Enterprise](https://gatling.io/enterprise/). 

## Versions

Check out available versions on [Maven Central](https://central.sonatype.com/search?q=g%253Aio.gatling%2520a%253Agatling-maven-plugin).

Beware that milestones (M versions) are not documented for OSS users and are only released for [Gatling Enterprise](https://gatling.io/enterprise/) customers.

## Setup

{{< alert tip >}}
Cloning or downloading one of our demo projects on GitHub is definitely the fastest way to get started:
* [for maven and Java](https://github.com/gatling/gatling-maven-plugin-demo-java)
* [for maven and Kotlin](https://github.com/gatling/gatling-maven-plugin-demo-kotlin)
* [for maven and Scala](https://github.com/gatling/gatling-maven-plugin-demo-scala)
{{< /alert >}}

If you prefer to manually configure your Maven project rather than clone one of our samples, you need to add the following to your `pom.xml`:

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

{{< alert warning >}}
**For Scala users only**: starting from version 4, this plugin no longer compiles Scala code and requires to use the `scala-maven-plugin` when using Gatling with Simulations written in Scala. Please check the `pom.xml` of the demo project for maven and Scala mentioned above for complete configuration.
{{< /alert >}}

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

#### API tokens

You need to configure an [an API token](https://gatling.io/docs/enterprise/cloud/reference/admin/api_tokens/) for most
of the tasks regarding Gatling Enterprise Cloud. The API token needs the `Configure` role.

Since you probably don’t want to include you secret token in your source code, you can configure it using either:

- the `GATLING_ENTERPRISE_API_TOKEN` environment variable
- the `gatling.enterprise.apiToken` [Java System property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)

If really needed, you can also configure it in your pom.xml:
```xml
<plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>${gatling-maven-plugin.version}</version>
    <configuration>
        <apiToken>YOUR_API_TOKEN</apiToken>
    </configuration>
</plugin>
```

#### Create or start a simulation

You can, using the `enterpriseStart` goal:

- configure a new simulation on Gatling Enterprise Cloud, upload your packaged code, and immediately start the simulation
- or, for a simulation already configured on Gatling Enterprise Cloud, upload any updated code and immediately start the simulation

{{< alert warning >}}
You will need to configure [an API token]({{< ref "#working-with-gatling-enterprise-cloud" >}}) with the `Configure` role.
{{< /alert >}}

Quick usage:

- configure and start a new simulation with `mvn gatling:enterpriseStart`, you will be prompted to choose all required
  options. This will also print the simulationId of the newly configured simulation.
- run the simulation again with `mvn gatling:enterpriseStart -Dgatling.enterprise.simulationId=<YOUR_SIMULATION_ID>`.
- run the simulation and wait for the end of the run (will regularly print out progress information, and exit with an error code if the simulation fails) with `mvn gatling:enterpriseStart -Dgatling.enterprise.simulationId=<YOUR_SIMULATION_ID> -Dgatling.enterprise.waitForRunEnd=true`.

List of configurations used by this task:

```xml
<plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>${gatling-maven-plugin.version}</version>
    <configuration>
        <!-- Simulation that needs to be started, you will be able to create a new simulation if empty -->
        <!-- You can also use the gatling.enterprise.simulationId system property -->
        <simulationId>YOUR_SIMULATION_ID</simulationId>
        <!-- Default package when creating a new simulation -->
        <!-- You can also use the gatling.enterprise.packageId system property -->
        <packageId>YOUR_PACKAGE_ID</packageId>
        <!-- Default team when creating a new simulation -->
        <!-- You can also use the gatling.enterprise.teamId system property -->
        <teamId>YOUR_TEAM_ID</teamId>
        <!-- Fully-qualified class name of the simulation used when creating a new simulation -->
        <!-- You can also use the gatling.simulationClass system property -->
        <simulationClass>io.gatling.BasicSimulation</simulationClass>
        <!-- Custom system properties used when running the simulation on Gatling Enterprise -->
        <simulationSystemProperties>
            <key1>VALUE_1</key1>
            <key2>VALUE_2</key2>
        </simulationSystemProperties>
        <!-- Additional environment variables used when running the simulation on Gatling Enterprise -->
        <simulationEnvironmentVariables>
            <key1>VALUE_1</key1>
            <key2>VALUE_2</key2>
        </simulationEnvironmentVariables>
        <!-- Wait for the result after starting the simulation on Gatling Enterprise -->
        <!-- Will complete with an error if the simulation ends with any error status -->
        <!-- False by default; you can also use the gatling.enterprise.waitForRunEnd system property -->
        <waitForRunEnd>true</waitForRunEnd>
    </configuration>
</plugin>
```

You can run it with the command:
```shell
mvn gatling:enterpriseStart
```

If a `simulationId` is set, the task will start the simulation on Gatling Enterprise.

If no `simulationId` is set, the task will ask you if you want to start or create a new simulation. If you choose
create, you will be able to configure a new simulation (with the configured `packageId`, `teamId`, `simulationClass` as
default), then start it. If you choose start, you will be able to start an already existing simulation on Gatling
Enterprise.

If you are on a CI environment, you don't want to handle interaction with the plugin. You should then run Maven in
[batch mode](https://maven.apache.org/ref/current/maven-embedder/cli.html#batch-mode). In batch mode, no input will be
asked from the user, the new simulation will be created using only the configuration.

#### Package

You can directly package your simulations for Gatling Enterprise Cloud using the `enterprisePackage` goal:

```shell
mvn gatling:enterprisePackage
```

This will generate the `target/<artifactId>-<version>-shaded.jar` package which you can then
[upload to the Cloud](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/).

#### Package and upload

You can also create and upload the package in a single command using the `enterpriseUpload` goal.

{{< alert warning >}}
You will need to configure [an API token]({{< ref "#working-with-gatling-enterprise-cloud" >}}) with the `Configure` role.
{{< /alert >}}

You must already have [configured a package](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/).
Copy the package ID from the Packages table, or copy the simulation ID linked to the package from the Simulations table.

Configure the package ID or simulation ID on the plugin:

```xml
<plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>${gatling-maven-plugin.version}</version>
    <configuration>
        <packageId>YOUR_PACKAGE_ID</packageId>
        <!-- If packageId is missing, the task will use the package linked to the simulationId -->
        <simulationId>YOUR_SIMULATION_ID</simulationId>
    </configuration>
</plugin>
```

You can also configure either of those using [Java System properties](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html):
- packageId: `gatling.enterprise.packageId`
- simulationId: `gatling.enterprise.simulationId`

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
