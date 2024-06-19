---
menutitle: sbt Plugin
title: Gatling sbt Plugin
seotitle: sbt Plugin for Gatling and Gatling Enterprise
description: How to use the sbt plugin for Gatling to run tests and deploy them to Gatling Enterprise.
lead: The sbt plugin allows you to run Gatling tests from the command line, without the bundle, as well as to package your simulations for Gatling Enterprise
aliases:
  - /reference/extensions/build-tools/sbt-plugin
date: 2021-04-20T18:30:56+02:00
lastmod: 2023-07-26T13:50:00+00:00
---

This sbt plugin integrates Gatling with sbt, allowing to use Gatling as a testing framework. It can also be used to
package your Gatling project to run it on [Gatling Enterprise](https://gatling.io/products/).

## Versions

Check out available versions on [Maven Central](https://central.sonatype.com/search?q=gatling-sbt&namespace=io.gatling).

Beware that milestones (M versions) are not documented for OSS users and are only released for [Gatling Enterprise](https://gatling.io/products/) customers.

## Setup

{{< alert warning >}}
This plugin only supports Simulations written in Scala. If you want to write your Simulations in Java or Kotlin, please
use [Maven]({{< ref "maven-plugin" >}}) or [Gradle]({{< ref "gradle-plugin" >}}).
{{< /alert >}}

{{< alert warning >}}
This plugin requires using sbt 1 (sbt 0.13 is not supported). All code examples on this page use the
[unified slash syntax](https://www.scala-sbt.org/1.x/docs/Migrating-from-sbt-013x.html#Migrating+to+slash+syntax)
introduced in sbt 1.1.
{{< /alert >}}

{{< alert tip >}}
Cloning or downloading our demo project on GitHub is definitely the fastest way to get started:
* [for sbt and Scala](https://github.com/gatling/gatling-sbt-plugin-demo)
{{< /alert >}}

If you prefer to manually configure your sbt project rather than clone our sample, you need to add the Gatling plugin dependency to your `project/plugins.sbt`:

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

This plugin offers two different custom sbt configurations, named `Gatling` and `GatlingIt`.
They are tied to different source directories (see next section for more details) and therefore allow to separate your simulations according to your needs, should you desire it.

Ideally:

* Your simulations with low injection profiles, which may serve as functional tests, should live in `src/test` (the default source directory for the `Gatling` configuration), and run along your unit tests, since they would complete quickly
* Longer, more complex simulations with high injection profiles, should live in `src/it` (the default source directory for the `GatlingIt` configuration) and be run on an as-needed basis.

Also, since they're tied to separate sbt configurations, your sbt settings can then be customized per configuration.
You can expect a relatively short simulation to run easily with the default JVM settings, but, for example, simulations with much higher load could require an increase of the max heap memory.

{{< alert tip >}}
When using the `GatlingIt` configuration, you must use the `GatlingIt/` prefix, e.g. `Gatling/test` becomes `GatlingIt/test`, etc...
{{< /alert >}}

### Default settings

For the `Gatling` configuration:

* By default, Gatling simulations must be in `src/test/scala`, configurable using the `Gatling / scalaSource` setting.
* By default, Gatling reports are written to `target/gatling`, configurable using the `Gatling / target` setting.

For the `GatlingIt` configuration:

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

As with any sbt testing framework, you'll be able to run Gatling simulations using sbt standard `test`, `testOnly`,
`testQuick`, etc... tasks. However, since the sbt Plugin introduces many customizations that we don't want to interfere
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

### Running your simulations on Gatling Enterprise Cloud

{{< alert info >}}
To work from the `it` configuration, simply replace `Gatling/` with `GatlingIt/` in the
configuration and commands.
{{< /alert >}}

#### Prerequisites

You need to configure an [an API token]({{< ref "reference/execute/cloud/admin/api-tokens" >}}) for most
of the actions between the CLI and Gatling Enterprise Cloud.

{{< alert warning >}}
The API token needs the `Configure` role on expected teams.
{{< /alert >}}

Since you probably don’t want to include you secret token in your source code, you can configure it using either:

- the `GATLING_ENTERPRISE_API_TOKEN` environment variable
- the `gatling.enterprise.apiToken` [Java System property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)

If really needed, you can also configure it in your build.sbt:
```scala
Gatling / enterpriseApiToken := "YOUR_API_TOKEN"
```

#### Deploying on Gatling Enterprise Cloud

With `Gatling/enterpriseDeploy` command, you can:
- Create, update and upload packages
- Create and update simulations

This command automatically checks your simulation project and performs the deployment according to your configuration.

By default, `enterpriseDeploy` searches for the package descriptor in `.gatling/package.conf`.
However, you can target a different filename in `.gatling` by using the following command:
```shell
sbt Gatling/enterpriseDeploy --package-descriptor-filename "<file name>"
```

{{< alert info >}}
You can run this command without any configuration to try it.

Check the [Configuration as Code documentation]({{< ref "reference/execute/cloud/user/configuration-as-code" >}}) for the complete reference and advanced usage.
{{< /alert >}}

#### Start your simulations on Gatling Enterprise Cloud

You can, using the `gatling:enterpriseStart` command:
- Automatically [deploy your package and associated simulations](#deploying-on-gatling-enterprise-cloud)
- Start a deployed simulation

By default, the Gatling plugin prompts the user to choose a simulation to start from among the deployed simulations.
However, users can also specify the simulation name directly to bypass the prompt using the following command:
```shell
sbt Gatling/enterpriseStart "<simulation name>"
```
Replace `<simulation name>` with the desired name of the simulation you want to start.

If you are on a CI environment, you don't want to handle interaction with the plugin.
Most CI tools define the `CI` environment variable, used by the Gatling plugin to disable interactions and run in headless mode.

If you need the command to wait until the run completes and to fail in case of assertion failures, you can enable:
```scala
Gatling / waitForRunEnd := true
```

Here are additional options for this command:
- `--run-title <title>`: Allows setting a title for your run reports.
- `--run-description <description>`: Allows setting a description for your run reports summary.

#### Upload a package manually

##### Packaging

You can directly package your simulations for Gatling Enterprise Cloud using:

```shell
sbt Gatling/enterprisePackage
```

This will generate the `target/gatling/<artifactId>-gatling-enterprise-<version>.jar` package which you can then
[upload to the Cloud]({{< ref "../../execute/cloud/user/package-conf" >}}).

To package simulations from the `it` configuration, `GatlingIt/enterprisePackage` will generate the
`target/gatling-it/<artifactId>-gatling-enterprise-<version>.jar` package.

##### Upload

You must already have [configured a package]({{< ref "../../execute/cloud/user/package-conf" >}}). Copy the package ID from
the Packages table, or copy the simulation ID linked to the package from the Simulations table.

Configure the package ID or simulation ID on the plugin:

```scala
Gatling / enterprisePackageId := "YOUR_PACKAGE_ID"
// If packageId is missing, the task will use the package linked to the simulationId
Gatling / enterpriseSimulationId := "YOUR_SIMULATION_ID"
```

You can also configure either of those using [Java System properties](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html):
- packageId: `gatling.enterprise.packageId`
- simulationId: `gatling.enterprise.simulationId`

Then package and upload your simulation to gatling Enterprise Cloud:

```shell
sbt Gatling/enterpriseUpload
```

To package and upload simulations from the `it` configuration, simply replace `Gatling/` with `GatlingIt/` in the
configuration and command.

#### Private packages

Configure the [Control Plane URL]({{< ref "/reference/install/cloud/private-locations/private-packages/#control-plane-server" >}}):

```scala
Gatling / enterpriseControlPlaneUrl := Some(URI.create("YOUR_CONTROL_PLANE_URL").toURL)
```

Once configured, your private package can be created and uploaded using the [deploy command]({{< ref "#deploying-on-gatling-enterprise-cloud" >}}).

### Additional tasks

Gatling's sbt plugin also offers four additional tasks:

* `Gatling/startRecorder`: starts the Recorder, configured to save recorded simulations to the location specified by `Gatling/scalaSource` (by default, `src/test/scala`).
* `Gatling/generateReport`: generates reports for a specified report folder.
* `Gatling/lastReport`: opens by the last generated report in your web browser. A simulation name can be specified to open the last report for that simulation.
* `Gatling/copyConfigFiles`: copies Gatling's configuration files (gatling.conf & recorder.conf) from the bundle into your project resources if they're missing.
* `Gatling/copyLogbackXml`: copies Gatling's default logback.xml.

## Overriding JVM options

Gatling's sbt plugin uses the same default JVM options as the bundle launchers or the Maven plugin, which should be sufficient for most simulations.
However, should you need to tweak them, you can use `overrideDefaultJavaOptions` to only override those default options, without replacing them completely.

E.g., if you want to tweak Xms/Xmx to give more memory to Gatling

```scala
Gatling / javaOptions := overrideDefaultJavaOptions("-Xms1024m", "-Xmx2048m")
```

## Sources

If you're interested in contributing, you can find the [gatling-sbt plugin sources](https://github.com/gatling/gatling-sbt) on GitHub.
