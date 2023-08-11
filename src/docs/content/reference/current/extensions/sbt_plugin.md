---
title: "SBT Plugin"
description: "How to use the sbt plugin for Gatling to run tests and deploy them to Gatling Enterprise."
lead: "The SBT plugin allows you to run Gatling tests from the command line, without the bundle, as well as to package your simulations for Gatling Enterprise"
date: 2021-04-20T18:30:56+02:00
lastmod: 2023-07-26T13:50:00+00:00
weight: 2080300
---

This SBT plugin integrates Gatling with SBT, allowing to use Gatling as a testing framework. It can also be used to
package your Gatling project to run it on [Gatling Enterprise](https://gatling.io/enterprise/).

## Versions

Check out available versions on [Maven Central](https://central.sonatype.com/search?q=gatling-sbt&namespace=io.gatling).

Beware that milestones (M versions) are not documented for OSS users and are only released for [Gatling Enterprise](https://gatling.io/enterprise/) customers.

## Setup

{{< alert warning >}}
This plugin only supports Simulations written in Scala. If you want to write your Simulations in Java or Kotlin, please
use [Maven]({{< ref "maven_plugin" >}}) or [Gradle]({{< ref "gradle_plugin" >}}).
{{< /alert >}}

{{< alert warning >}}
This plugin requires using SBT 1 (SBT 0.13 is not supported). All code examples on this page use the
[unified slash syntax](https://www.scala-sbt.org/1.x/docs/Migrating-from-sbt-013x.html#Migrating+to+slash+syntax)
introduced in SBT 1.1.
{{< /alert >}}

{{< alert tip >}}
Cloning or downloading our demo project on GitHub is definitely the fastest way to get started:
* [for sbt and Scala](https://github.com/gatling/gatling-sbt-plugin-demo)
{{< /alert >}}

If you prefer to manually configure your SBT project rather than clone our sample, you need to add the Gatling plugin dependency to your `project/plugins.sbt`:

```scala
addSbtPlugin("io.gatling" % "gatling-sbt" % "MANUALLY_REPLACE_WITH_LATEST_VERSION")
```

And then add the Gatling library dependencies and enable the Gatling plugin in your `build.sbt`:

```scala
enablePlugins(GatlingPlugin)
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "MANUALLY_REPLACE_WITH_LATEST_VERSION" % "test"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "MANUALLY_REPLACE_WITH_LATEST_VERSION" % "test"
```

### 'Test' vs 'Integration Tests' configurations

This plugin offers two different custom SBT configurations, named `Gatling` and `GatlingIt`.
They are tied to different source directories (see next section for more details) and therefore allow to separate your simulations according to your needs, should you desire it.

Ideally:

* Your simulations with low injection profiles, which may serve as functional tests, should live in `src/test` (the default source directory for the `Gatling` configuration), and run along your unit tests, since they would complete quickly
* Longer, more complex simulations with high injection profiles, should live in `src/it` (the default source directory for the `GatlingIt` configuration) and be run on an as-needed basis.

Also, since they're tied to separate SBT configurations, your SBT settings can then be customized per configuration.
You can expect a relatively short simulation to run easily with the default JVM settings, but, for example, simulations with much higher load could require an increase of the max heap memory.

{{< alert tip >}}
When using the `GatlingIt` configuration, you must use the `GatlingIt/` prefix, e.g. `Gatling/test` becomes `GatlingIt/test`, etc...
{{< /alert >}}

### Default settings

For the `Gatling` configuration :

* By default, Gatling simulations must be in `src/test/scala`, configurable using the `Gatling / scalaSource` setting.
* By default, Gatling reports are written to `target/gatling`, configurable using the `Gatling / target` setting.

For the `GatlingIt` configuration :

* By default, Gatling simulations must be in `src/it/scala`, configurable using the `GatlingIt / scalaSource` setting.
* By default, Gatling reports are written to `target/gatling-it`, configurable using the `GatlingIt / target` setting.

If you override the default settings, you need to reset them on the project, eg:

```scala
Gatling / scalaSource := sourceDirectory.value / "gatling" / "scala"
lazy val root = (project in file(".")).settings(inConfig(Gatling)(Defaults.testSettings): _*)
```

### Multi-project support

If you have a [multi-project build](https://www.scala-sbt.org/1.x/docs/Multi-Project.html), make sure to only configure
the subprojects which contain Gatling Simulations with the Gatling plugin and dependencies as described above. Your
Gatling subproject can, however, depend on other subprojects.

## Usage

### Running your simulations

As with any SBT testing framework, you'll be able to run Gatling simulations using SBT standard `test`, `testOnly`,
`testQuick`, etc... tasks. However, since the SBT Plugin introduces many customizations that we don't want to interfere
with unit tests, those commands are integrated into custom configurations, meaning you'll need to prefix them with
`Gatling/` or `GatlingIt/`.

For example, run all Gatling simulations from the `test` configuration:

```bash
sbt Gatling/test
```

Or run a single simulation, by its FQN (fully qualified class name), from the `it` configuration:

```bash
sbt 'GatlingIt/testOnly com.project.simu.MySimulation'
```

{{< alert tip >}}
This behavior differs from what was previously possible, eg. calling `test` without prefixing started Gatling simulations.
However, this caused many interferences with other testing libraries and forcing the use of a prefix solves those issues.
{{< /alert >}}

### Working with Gatling Enterprise Cloud

{{< alert info >}}
To work from the `it` configuration, simply replace `Gatling/` with `GatlingIt/` in the
configuration and commands.
{{< /alert >}}

#### API tokens

You need to configure an [an API token](https://gatling.io/docs/enterprise/cloud/reference/admin/api_tokens/) for most of the tasks regarding Gatling Enterprise Cloud. The API token needs the `Configure` role.

Since you probably don’t want to include you secret token in your source code, you can configure it using either:

- the `GATLING_ENTERPRISE_API_TOKEN` environment variable
- the `gatling.enterprise.apiToken` [Java System property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)

If really needed, you can also configure it in your build.sbt:
```scala
Gatling / enterpriseApiToken := "YOUR_API_TOKEN"
```

#### Create or start a simulation

You can, using the `Gatling/enterpriseStart` task:

- configure a new simulation on Gatling Enterprise Cloud, upload your packaged code, and immediately start the simulation
- or, for a simulation already configured on Gatling Enterprise Cloud, upload any updated code and immediately start the simulation

{{< alert warning >}}
You will need to configure [an API token]({{< ref "#working-with-gatling-enterprise-cloud" >}}) with the `Configure` role.
{{< /alert >}}

Quick usage:

- configure and start a new simulation with `sbt Gatling/enterpriseStart`, you will be prompted to choose all required
  options. This will also print the simulationId of the newly configured simulation.
- run the simulation again with `sbt Gatling/enterpriseStart -Dgatling.enterprise.simulationId=<YOUR_SIMULATION_ID>`.
- run the simulation and wait for the end of the run (will regularly print out progress information, and exit with an error code if the simulation fails) with `sbt Gatling/enterpriseStart -Dgatling.enterprise.simulationId=<YOUR_SIMULATION_ID> -Dgatling.enterprise.waitForRunEnd=true`.

List of configurations used by this task:

```scala
// You can also use the gatling.enterprise.simulationId system property
Gatling / enterpriseSimulationId := "YOUR_SIMULATION_ID"
// You can also use the gatling.enterprise.packageId system property
Gatling / enterprisePackageId := "YOUR_PACKAGE_ID"
// You can also use the gatling.enterprise.teamId system property
Gatling / enterpriseTeamId := "YOUR_TEAM_ID"
// default simulation fully qualified classname used when creating a new simulation, you can also use the gatling.enterprise.simulationClass system property
Gatling / enterpriseSimulationClass := "computerdatabase.BasicSimulation"
// custom system properties used when running the simulation on Gatling Enterprise
Gatling / enterpriseSimulationSystemProperties := Map.empty
// Additional environment variables used when running the simulation on Gatling Enterprise
Gatling / enterpriseSimulationEnvironmentVariables := Map.empty
// Wait for the result after starting the simulation on Gatling Enterprise; will complete with an error if the simulation ends with any error status
// False by default; you can also use the gatling.enterprise.waitForRunEnd system property
Gatling / waitForRunEnd := false
// If this URL is configured, newly created packages and uploaded ones are considered as private.
// Private packages are uploaded and managed through this control plane.
// See Private Packages on Gatling Cloud documentation for details :
// https://gatling.io/docs/enterprise/cloud/reference/admin/private_locations/private_packages/
Gatling / enterpriseControlPlaneUrl := Some(new URL("YOUR_CONTROL_PLANE_URL"))
```

You can run it with the command:
```shell
sbt Gatling/enterpriseStart
```

If a `simulationId` is set, the task will start the simulation on Gatling Enterprise.

If no simulationId is set, the task will ask you if you want to start or create a new simulation. If you choose create, you will be able to configure a new simulation (with the configured `packageId`, `teamId`, `simulationClass` as default), then start it. If you choose start, you will be able to start an already existing simulation on Gatling Enterprise.

If you are on a CI environment, you don't want to handle interaction with the plugin. You should then set the `batchMode` option to true. In batch mode, no input will be asked from the user, the new simulation will be created using only the configuration.

#### Package

You can directly package your simulations for Gatling Enterprise Cloud:

```shell
sbt Gatling/enterprisePackage
```

This will generate the `target/gatling/<artifactId>-gatling-enterprise-<version>.jar` package which you can then
[upload to the Cloud](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/).

To package simulations from the `it` configuration, `GatlingIt/enterprisePackage` will generate the
`target/gatling-it/<artifactId>-gatling-enterprise-<version>.jar` package.

#### Package and upload

{{< alert warning >}}
You will need to configure [an API token]({{< ref "#working-with-gatling-enterprise-cloud" >}}) with the `Configure` role.
{{< /alert >}}

You must already have [configured a package](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/). Copy the package ID from
the Packages table, or copy the simulation ID linked to the package from the Simulations table.

Configure the package ID or simulation ID on the plugin:

```scala
Gatling / enterprisePackageId := "YOUR_PACKAGE_ID"
// omit enterpriseApiToken when using environment variable or Java System property instead
Gatling / enterpriseApiToken := "YOUR_API_TOKEN"
```

Then package and upload your simulation to gatling Enterprise Cloud:

```shell
sbt Gatling/enterpriseUpload
```

To package and upload simulations from the `it` configuration, simply replace `Gatling/` with `GatlingIt/` in the
configuration and command.

### Working with Gatling Enterprise Self-Hosted

#### Build from sources

Once you have configured the SBT plugin on your project, Gatling Enterprise Self-Hosted can build it from sources
without additional configuration.
[Add your source repository](https://gatling.io/docs/enterprise/self-hosted/reference/current/user/repositories/#sources-repository)
and configure your simulation to
[build from sources](https://gatling.io/docs/enterprise/self-hosted/reference/current/user/simulations/#option-1-build-from-sources)
using SBT.

To make sure your setup is correct, you can run the packaging command and check that you get a jar containing all the
classes and extra dependencies of your project in `target/gatling/<artifactId>-gatling-enterprise-<version>.jar`:

```shell
sbt Gatling/enterprisePackage
```

{{< alert warning >}}
If you use the `it` configuration, you will need to configure a custom build command in Gatling Enterprise, as the
default one is for the `test` configuration:
``sbt -J-Xss100M ;clean;GatlingIt/enterprisePackage -batch --error``
{{< /alert >}}

#### Publish to a binary repository

Alternatively, you can package your simulations and publish them to a binary repository (JFrog Artifactory, Sonatype
Nexus or AWS S3).

{{< alert tip >}}
Please refer to the [official documentation](https://www.scala-sbt.org/1.x/docs/Publishing.html) for generic
configuration options. Please also check the standards within your organization for the best way to configure the
credentials needed to access your binary repository.
{{< /alert >}}

Enable publishing the Gatling test artifact, then define the repository:

```scala
Gatling / publishArtifact := true
publishTo := (
  if (isSnapshot.value)
    Some("private repo" at "REPLACE_WITH_YOUR_SNAPSHOTS_REPOSITORY_URL")
  else
    Some("private repo" at "REPLACE_WITH_YOUR_RELEASES_REPOSITORY_URL")
)
```

The packaged artifact will be automatically attached to your project and deployed with the `tests` classifier when you publish it:

```shell
sbt publish
```

You can also set:
- `GatlingIt / publishArtifact := true` to publish Gatling simulations from the `it` configuration, this artifact will be
published with the `it` qualifier
- `Compile / publishArtifact := false` e.g. if your project only contains Gatling simulations and you don't need to
publish code from `src/main`.

### Additional tasks

Gatling's SBT plugin also offers four additional tasks:

* `Gatling/startRecorder`: starts the Recorder, configured to save recorded simulations to the location specified by `Gatling/scalaSource` (by default, `src/test/scala`).
* `Gatling/generateReport`: generates reports for a specified report folder.
* `Gatling/lastReport`: opens by the last generated report in your web browser. A simulation name can be specified to open the last report for that simulation.
* `Gatling/copyConfigFiles`: copies Gatling's configuration files (gatling.conf & recorder.conf) from the bundle into your project resources if they're missing.
* `Gatling/copyLogbackXml`: copies Gatling's default logback.xml.

## Overriding JVM options

Gatling's SBT plugin uses the same default JVM options as the bundle launchers or the Maven plugin, which should be sufficient for most simulations.
However, should you need to tweak them, you can use `overrideDefaultJavaOptions` to only override those default options, without replacing them completely.

E.g., if you want to tweak Xms/Xmx to give more memory to Gatling

```scala
Gatling / javaOptions := overrideDefaultJavaOptions("-Xms1024m", "-Xmx2048m")
```

## Sources

If you're interested in contributing, you can find the [gatling-sbt plugin sources](https://github.com/gatling/gatling-sbt) on GitHub.
