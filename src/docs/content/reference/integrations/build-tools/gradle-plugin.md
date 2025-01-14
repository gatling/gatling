---
menutitle: Gradle Plugin
title: Gatling Gradle Plugin
seotitle: Gradle Plugin for Gatling and Gatling Enterprise
description: How to use the Gradle plugin for Gatling to run tests and deploy them to Gatling Enterprise.
lead: The Gradle plugin allows you to run Gatling tests from the command line, without the bundle, as well as to package your simulations for Gatling Enterprise
aliases:
  - /reference/extensions/build-tools/gradle-plugin
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

This Gradle plugin was initially contributed by [Ievgenii Shepeliuk](https://github.com/eshepelyuk) and
[Laszlo Kishalmi](https://github.com/lkishalmi).

This Gradle plugin integrates Gatling with Gradle, allowing to use Gatling as a testing framework.

## Versions

Check out available versions on [Gradle Plugins Portal](https://plugins.gradle.org/plugin/io.gatling.gradle).

## Compatibility

### Gradle version

{{< alert warning >}}
This plugin requires at least Gradle 7.6.
{{< /alert >}}

The latest version of this plugin is tested against Gradle versions ranging from 7.1 to 8.6.
Any version outside this range is not guaranteed to work.

## Setup

{{< alert tip >}}
Cloning or downloading one of our demo projects on GitHub is definitely the fastest way to get started:
* [for gradle and Java](https://github.com/gatling/gatling-gradle-plugin-demo-java)
* [for gradle and Kotlin](https://github.com/gatling/gatling-gradle-plugin-demo-kotlin)
* [for gradle and Scala](https://github.com/gatling/gatling-gradle-plugin-demo-scala)

They also come pre-configured with the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html), so you don't need to install Gradle yourself.
{{< /alert >}}

If you prefer to manually configure your Gradle project rather than clone one of our samples, you need to add the following to your `build.gradle`:

```groovy
 plugins {
   id 'io.gatling.gradle' version "MANUALLY_REPLACE_WITH_LATEST_VERSION"
 }
```

### Multi-project support

If you have a [multi-project build](https://docs.gradle.org/current/userguide/multi_project_builds.html), make sure to
only configure the subprojects which contain Gatling Simulations with the Gatling plugin as described above. Your
Gatling subproject can, however, depend on other subprojects.

## Source files layout

The plugin creates a dedicated [Gradle sourceSet](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html)
named `gatling`. This source set is used for storing simulations and Gatling configurations. The following directories
are configured by default.

| Directory               | Purpose                                         |
| ----------------------- | ----------------------------------------------- |
| `src/gatling/java`      | Simulation sources (Java code)                  |
| `src/gatling/kotlin`    | Simulation sources (Kotlin code)                |
| `src/gatling/scala`     | Simulation sources (Scala code)                 |
| `src/gatling/resources` | Resources (feeders, configuration, bodies, etc) |

Using the Gradle API, file locations can be customized.

```groovy
sourceSets {
  gatling {
    scala.srcDir "folder1" <1>
    // or
    scala.srcDirs = ["folder1"] <2>

    resources.srcDir "folder2" <3>
    // or
    resources.srcDirs = ["folder2"] <4>
  }
}
```

1. append `folder1` as an extra simulations' folder.
2. use `folder1` as a single source of simulations.
3. append `folder2` as an extra `Gatling` resources folder.
4. use `folder2` as a single source of `Gatling` resources.

## Plugin configuration

The plugin defines the following extension properties in the `gatling` closure:

| Property name       | Type    | Default value                                                                                                                                                                                                                                                | Description                                                                                     |
|---------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `gatlingVersion`    | String  | The first 3 digits of this plugin's version                                                                                                                                                                                                                  | Gatling version                                                                                 |
| `includeMainOutput` | Boolean | `true`                                                                                                                                                                                                                                                       | `true`                                                                                          |
| `includeTestOutput` | Boolean | `true`                                                                                                                                                                                                                                                       | Include test source set output to gatlingImplementation                                         |
| `scalaVersion`      | String  | `'2.13.16'`                                                                                                                                                                                                                                                  | Scala version that fits your Gatling version                                                    |
| `jvmArgs`           | List    | <pre>[<br> '-server',<br> '-Xmx1G',<br> '-XX:+HeapDumpOnOutOfMemoryError',<br> '-XX:MaxInlineLevel=20',<br> '-XX:MaxTrivialSize=12',<br> '--add-opens=java.base/java.lang=ALL-UNNAMED',<br> '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED'<br>]</pre> | Arguments passed to JVM when executing Gatling simulations (setting them replaces the defaults) |
| `systemProperties`  | Map     | `[]`                                                                                                                                                                                                                                                         | Systems properties passed to JVM together with gradle ones                                      |
| `simulation`        | String  | A fully qualified class name that extends a Gatling `Simulation`                                                                                                                                                                                             | The simulation to run                                                                           |
| `apiToken`          | String  | `null`, optional                                                                                                                                                                                                                                             | Your Gatling Enterprise api token                                                               |

How to override Gatling version, JVM arguments and system properties:

```groovy
gatling {
  gatlingVersion = '3.13.1'
  jvmArgs = ['-server', '-Xms512M', '-Xmx512M']
  systemProperties = ['file.encoding': 'UTF-8']
}
```

## Gatling configuration

### Override gatling.conf settings

To override Gatling's
[default parameters](https://github.com/gatling/gatling/blob/main/gatling-core/src/main/resources/gatling-defaults.conf),
put your own version of `gatling.conf` into `src/gatling/resources`.

### Logging management

Gatling uses [Logback](http://logback.qos.ch/documentation.html). To change the logging behaviour, put your
custom `logback.xml` configuration file in the resources folder, `src/gatling/resources`.

## Dependency management

This plugin defines three [Gradle configurations](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html):
`gatling`, `gatlingImplementation` and `gatlingRuntimeOnly`.

By default, the plugin adds Gatling libraries to `gatling` configuration.
Configurations `gatlingImplementation` and `gatlingRuntimeOnly` extend `gatling`,
i.e. all dependencies declared in `gatling` will be inherited. Dependencies added
to configurations other than these '`gatling`' configurations will not be available
within Gatling simulations.

Also, project classes (`src/main`) and tests classes (`src/test`) are added to
`gatlingImplementation` and `gatlingRuntimeOnly` classpath, so you can reuse
existing production and test code in your simulations.

If you do not need such behaviour, you can use flags. Manage test and main output:

```groovy
gatling {
  // do not include classes and resources from src/main
  includeMainOutput = false
  // do not include classes and resources from src/test
  includeTestOutput = false
}
```

Additional dependencies can be added to any of the configurations mentioned above. Add external libraries for `Gatling`
simulations:

```groovy
dependencies {
  gatling 'com.google.code.gson:gson:2.8.0' // <1>
  gatlingImplementation 'org.apache.commons:commons-lang3:3.4' // <2>
  gatlingRuntimeOnly 'cglib:cglib-nodep:3.2.0' // <3>
}
```

1. adding gson library, available both in compile and runtime classpath.
2. adding commons-lang3 to compile classpath for simulations.
3. adding cglib to runtime classpath for simulations.

## Tasks

### Running your simulations

Use the task `GatlingRunTask` to execute Gatling simulations. You can create your own instances of this task
to run particular simulations, or use the default `gatlingRun` task.

By default, the `gatlingRun` task runs in interactive mode and suggests the simulation class to launch unless:
* there's only one Simulation available,
* or the Simulation class is forced with the `--simulation=<FullyQualifiedClassName>` option,
* or the non-interactive mode is forced with the `--non-interactive` option, in which case the task will fail if there is more than 1 simulation available,
* or the `CI` environment variable is set to true, in which case the task will fail if there is more than 1 simulation available.

For example, to run a simulation:

{{< platform-toggle >}}
Linux/MacOS: ./gradlew gatlingRun
Windows: gradlew.bat gatlingRun
{{</ platform-toggle >}}

Run a single simulation by its FQN (fully qualified class name):

{{< platform-toggle >}}
Linux/MacOS: ./gradlew gatlingRun --simulation com.project.simu.MySimulation
Windows: gradlew.bat gatlingRun --simulation com.project.simu.MySimulation
{{</ platform-toggle >}}

You can run all simulations with the `--all` option:

{{< platform-toggle >}}
Linux/MacOS: ./gradlew gatlingRun --all
Windows: gradlew.bat gatlingRun --all
{{</ platform-toggle >}}

The following configuration options are available. Those options are similar to
global `gatling` configurations. Options are used in a fallback manner, i.e. if
an option is not set the value from the `gatling` global config is taken.

| Property name      | Type | Default value | Description |
|--------------------| --- | --- | --- |
| `simulation`       | String  | The only class that extends a Gatling `Simulation` | The simulation to run |
| `jvmArgs`          | List<String>        | `null` | Additional arguments passed to JVM when executing Gatling simulations |
| `systemProperties` | Map<String, Object> | `null` | Additional systems properties passed to JVM together with caller JVM system properties |
| `environment`      | Map<String, Object> | `null` | Additional environment variables passed to the simulation |

### Running the Gatling Recorder

You can launch the [Gatling Recorder]({{< ref "../../script/protocols/http/recorder" >}}):

{{< platform-toggle >}}
Linux/MacOS: ./gradlew gatlingRecorder
Windows: gradlew.bat gatlingRecorder
{{</ platform-toggle >}}

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

If really needed, you can also configure it in your build.gradle:

```groovy
gatling {
  enterprise {
    apiToken "YOUR_API_TOKEN"
  }
}
```

#### Deploying on Gatling Enterprise Cloud

With `gatlingEnterpriseDeploy` command, you can:
- Create, update and upload packages
- Create and update simulations

This command automatically checks your simulation project and performs the deployment according to your configuration.

By default, `GatlingEnterpriseDeploy` searches for the package descriptor in `.gatling/package.conf`.
However, you can target a different filename in `.gatling` by using the following command:
```shell
gradle GatlingEnterpriseDeploy -Dgatling.enterprise.packageDescriptorFilename="<file name>"
```

{{< alert info >}}
You can run this command without any configuration to try it.

Check the [Configuration as Code documentation]({{< ref "reference/execute/cloud/user/configuration-as-code" >}}) for the complete reference and advanced usage.
{{< /alert >}}


#### Start your simulations on Gatling Enterprise Cloud

You can, using the `gatlingEnterpriseStart` command:
- Automatically [deploy your package and associated simulations](#deploying-on-gatling-enterprise-cloud)
- Start a deployed simulation

By default, the Gatling plugin prompts the user to choose a simulation to start from amongst the deployed simulations.
However, users can also specify the simulation name directly to bypass the prompt using the following command:

{{< platform-toggle >}}
Linux/MacOS: ./gradlew gatlingEnterpriseStart -Dgatling.enterprise.simulationName="<simulation name>"
Windows: gradlew.bat gatlingEnterpriseStart -Dgatling.enterprise.simulationName="<simulation name>"
{{</ platform-toggle >}}

Replace `<simulation name>` with the desired name of the simulation you want to start.

If you are on a CI environment, you don't want to handle interaction with the plugin.
Most CI tools define the `CI` environment variable, used by the Gatling plugin to disable interactions and run in headless mode.

It's also possible to disable interactions by setting `-Dgatling.enterprise.batchMode=true`.

{{< alert tip >}}
Lifecycle logs need to be enabled in interactive mode.
{{< /alert >}}

Here are additional options for this command:
- `-Dgatling.enterprise.waitForRunEnd=true`: Enables the command to wait until the run finishes and fail if there are assertion failures.
- `-Dgatling.enterprise.runTitle=<title>`: Allows setting a title for your run reports.
- `-Dgatling.enterprise.runDescription=<description>`:  Allows setting a description for your run reports summary.

#### Upload a package manually

##### Packaging

You can directly package your simulations for Gatling Enterprise Cloud using:

{{< platform-toggle >}}
Linux/MacOS: ./gradlew gatlingEnterprisePackage
Windows: gradlew.bat gatlingEnterprisePackage
{{</ platform-toggle >}}

This will generate the `build/libs/<artifactId>-<version>-tests.jar` package which you can then
[upload to the Cloud]({{< ref "reference/execute/cloud/user/package-conf" >}}).

##### Upload

You must already have [configured a package]({{< ref "reference/execute/cloud/user/package-conf" >}}). Copy the package ID from
the Packages table, or copy the simulation ID linked to the package from the Simulations table.

Configure the package ID or simulation ID on the plugin:

```groovy
gatling {
  enterprise {
    packageId "YOUR_PACKAGE_ID"
    simulationId "YOUR_SIMULATION_ID" // If packageId is missing, the task will use the package linked to the simulationId
  }
}
```

You can also configure either of those using [Java System properties](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html):
- packageId: `gatling.enterprise.packageId`
- simulationId: `gatling.enterprise.simulationId`

Then package and upload your simulation to Gatling Enterprise Cloud:

{{< platform-toggle >}}
Linux/MacOS: ./gradlew gatlingEnterpriseUpload
Windows: gradlew.bat gatlingEnterpriseUpload
{{</ platform-toggle >}}

#### Private packages

Configure the [Control Plane URL]({{< ref "/reference/install/cloud/private-locations/private-packages/#control-plane-server" >}}):

```yaml
gatling {
  enterprise {
    controlPlaneUrl "YOUR_CONTROL_PLANE_URL"
  }
}
```

Once configured, your private package can be created and uploaded using the [deploy command]({{< ref "#deploying-on-gatling-enterprise-cloud" >}}).

## Sources

If you're interested in contributing, you can find the [io.gatling.gradle plugin sources](https://github.com/gatling/gatling-gradle-plugin) on GitHub.
