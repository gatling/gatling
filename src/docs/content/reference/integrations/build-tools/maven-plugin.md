---
menutitle: Maven Plugin
title: Gatling Maven Plugin
seotitle: Maven Plugin for Gatling and Gatling Enterprise
description: How to use the Maven plugin for Gatling to run tests and deploy them to Gatling Enterprise.
lead: The Maven plugin allows you to run Gatling tests from the command line, without the bundle, as well as to package your simulations for Gatling Enterprise
aliases:
  - /reference/extensions/build-tools/maven-plugin
date: 2021-04-20T18:30:56+02:00
lastmod: 2023-07-26T13:50:00+00:00
---

Using this plugin, Gatling can be launched when building your project, for example with your favorite Continuous Integration (CI) solution.
This plugin can also be used to package your Gatling project to run it on [Gatling Enterprise](https://gatling.io/enterprise/). 

## Versions

Check out available versions on [Maven Central](https://central.sonatype.com/search?q=gatling-maven-plugin&namespace=io.gatling).

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
**For Scala users only**: starting from version 4, this plugin no longer compiles Scala code and requires to use the `scala-maven-plugin` when using Gatling with Simulations written in Scala. Please check the `pom.xml` of the demo project for Maven and Scala mentioned above for complete configuration.
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

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:test
Windows: mvnw.cmd gatling:test
{{</ code-toggle >}}

Use `mvn gatling:help -Ddetail=true -Dgoal=test` to print the description of all the available configuration options on
the `test` goal.

The `gatling:test` goal runs in interactive mode and suggests the simulation class to launch unless:
* there's only one simulation available,
* or the Simulation class is forced with the `-Dgatling.simulationClass=<FullyQualifiedClassName>` Java System Property,
* or the non-interactive mode is forced, in which case the task will fail if there is more than 1 simulation available,
* or it's in batch mode (`-B` Maven option), in which case the task will fail if there is more than 1 simulation available,
* or the `CI` env var is set to `true`, in which case the task will fail if there is more than 1 simulation available.

### Running the Gatling Recorder

You can launch the [Gatling Recorder]({{< ref "../../script/protocols/http/recorder" >}}):

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:recorder
Windows: mvnw.cmd gatling:recorder
{{</ code-toggle >}}

Use `gatling:help -Ddetail=true -Dgoal=recorder` to print the description of all the available configuration options
on the `recorder` goal.

### Running your simulations on Gatling Enterprise Cloud

#### Prerequisites

You need to configure an [an API token]({{< ref "reference/execute/cloud/admin/api-tokens" >}}) for most
of the actions between the CLI and Gatling Enterprise Cloud.

{{< alert warning >}}
The API token needs the `Configure` role on expected teams.
{{< /alert >}}

Since you probably don’t want to include you secret token in your source code, you can configure it using either:

- the `GATLING_ENTERPRISE_API_TOKEN` environment variable
- the `gatling.enterprise.apiToken` [Java System property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)

{{< alert info >}}
Learn how to work with environment variables and Java system properties in the [Configuration docummentation]({{< ref "/reference/script/core/configuration#manage-configuration-values" >}}). 
{{< /alert >}}

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

#### Deploying on Gatling Enterprise Cloud

With `gatling:enterpriseDeploy` command, you can:
- Create, update and upload packages
- Create and update simulations

This command automatically checks your simulation project and performs the deployment according to your configuration.

By default, `enterpriseDeploy` searches for the package descriptor in `.gatling/package.conf`.
However, you can target a different filename in `.gatling` by using the following command:
```shell
mvn gatling:enterpriseDeploy -Dgatling.enterprise.packageDescriptorFilename="<file name>"
```

{{< alert info >}}
You can run this command without any configuration to try it.

Check the [Configuration as Code documentation]({{< ref "reference/execute/cloud/user/configuration-as-code" >}}) for the complete reference and advanced usage.
{{< /alert >}}

#### Start your simulations on Gatling Enterprise Cloud

You can, using the `gatling:enterpriseStart` command:
- Automatically [deploy your package and associated simulations](#deploying-on-gatling-enterprise-cloud)
- Start a deployed simulation

By default, the Gatling plugin prompts the user to choose a simulation to start from amongst the deployed simulations.
However, users can also specify the simulation name directly to bypass the prompt using the following command:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:enterpriseStart -Dgatling.enterprise.simulationName="<simulation name>"
Windows: mvnw.cmd gatling:enterpriseStart -Dgatling.enterprise.simulationName="<simulation name>"
{{</ code-toggle >}}

Replace `<simulation name>` with the desired name of the simulation you want to start.

If you are on a CI environment, you don't want to handle interaction with the plugin.
Most CI tools define the `CI` environment variable, used by the Gatling plugin to disable interactions and run in headless mode.

It's also possible to disable interactions by running Maven in [batch mode](https://maven.apache.org/ref/current/maven-embedder/cli.html#batch-mode).

Here are additional options for this command:
- `-Dgatling.enterprise.waitForRunEnd=true`: Enables the command to wait until the run finishes and fail if there are assertion failures.
- `-Dgatling.enterprise.runTitle=<title>`: Allows setting a title for your run reports.
- `-Dgatling.enterprise.runDescription=<description>`:  Allows setting a description for your run reports summary.

#### Upload a package manually

##### Packaging

You can directly package your simulations for Gatling Enterprise Cloud using:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:enterprisePackage
Windows: mvnw.cmd gatling:enterprisePackage
{{</ code-toggle >}}

This will generate the `target/<artifactId>-<version>-shaded.jar` package which you can then
[upload to the Cloud]({{< ref "reference/execute/cloud/user/package-conf" >}}).

##### Upload

You must already have [configured a package]({{< ref "reference/execute/cloud/user/package-conf" >}}).
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

Then package and upload your simulation to Gatling Enterprise Cloud:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:enterpriseUpload
Windows: mvnw.cmd gatling:enterpriseUpload
{{</ code-toggle >}}

#### Private packages

Configure the [Control Plane URL]({{< ref "/reference/install/cloud/private-locations/private-packages/#control-plane-server" >}}):

```scala
<plugin>
  <groupId>io.gatling</groupId>
  <artifactId>gatling-maven-plugin</artifactId>
  <version>${gatling-maven-plugin.version}</version>
  <configuration>
    <controlPlaneUrl>YOUR_CONTROL_PLANE_URL</controlPlaneUrl>
  </configuration>
</plugin>
```

Once configured, your private package can be created and uploaded using the [deploy command]({{< ref "#deploying-on-gatling-enterprise-cloud" >}}).

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
