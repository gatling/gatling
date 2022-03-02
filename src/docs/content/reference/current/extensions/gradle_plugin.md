---
title: "Gradle Plugin"
description: "Gradle plugin to run Gatling tests and deploy to Gatling Enterprise"
lead: "The Gradle plugin allows you to run Gatling tests from the command line, without the bundle, as well as to package your simulations for Gatling Enterprise"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-11-23T09:00:00+02:00
weight: 2080200
---

This Gradle plugin was initially contributed by [Ievgenii Shepeliuk](https://github.com/eshepelyuk) and
[Laszlo Kishalmi](https://github.com/lkishalmi).

This Gradle plugin integrates Gatling with Gradle, allowing to use Gatling as a testing framework.

## Versions

Check out available versions on [Gradle Plugins Portal](https://plugins.gradle.org/plugin/io.gatling.gradle).

## Compatibility

### Gradle version

{{< alert warning >}}
This plugin requires at least Gradle 5.
{{< /alert >}}

The latest version of this plugin is tested against Gradle versions ranging from 5.0.0 to 7.3.
Any version outside this range is not guaranteed to work.

## Setup

Install [Gradle](https://gradle.org/install/) or use the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).
Our official sample projects come pre-configured with the Gradle Wrapper.

In `build.gradle`, add:

```groovy
 plugins {
   id 'io.gatling.gradle' version "MANUALLY_REPLACE_WITH_LATEST_VERSION"
 }
```

{{< alert tip >}}
Cloning or downloading one of our demo projects on GitHub is definitely the fastest way to get started:
* [for gradle and Java](https://github.com/gatling/gatling-gradle-plugin-demo-java)
* [for gradle and Kotlin](https://github.com/gatling/gatling-gradle-plugin-demo-kotlin)
* [for gradle and Scala](https://github.com/gatling/gatling-gradle-plugin-demo-scala)
{{< /alert >}}

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

| Property name | Type | Default value  | Description |
| --- | --- | --- | --- |
| `gatlingVersion`    | String  | The first 3 digits of this plugin's version | Gatling version |
| `logLevel`          | String  | `'WARN'` | The default Gatling console log level if no `logback.xml` present in the configuration folder |
| `logHttp`           | String  | `'NONE'` | Verbosity of logging HTTP requests performed by Gatling, must be one of: <br/> * `'NONE'` - do not log, <br/> * `'ALL'` - log all requests, <br/> * `'FAILURES'` - only failed requests |
| `includeMainOutput` | Boolean | `true` | `true` |
| `includeTestOutput` | Boolean | `true` | Include test source set output to gatlingImplementation |
| `scalaVersion`      | String  | `'2.13.7'` | Scala version that fits your Gatling version |
| `jvmArgs`           | List    | <pre>[<br> '-server',<br> '-Xmx1G',<br> '-XX:+HeapDumpOnOutOfMemoryError',<br> '-XX:+UseG1GC',<br>  '-XX:+ParallelRefProcEnabled',<br> '-XX:MaxInlineLevel=20',<br> '-XX:MaxTrivialSize=12',<br> '-XX:-UseBiasedLocking'<br>]</pre> | Additional arguments passed to JVM when executing Gatling simulations |
| `systemProperties`  | Map     | `['java.net.preferIPv6Addresses': true]` | Additional systems properties passed to JVM together with caller JVM system properties |
| `simulations`       | Closure | `include("**/*Simulation*.java", "**/*Simulation*.kt", "**/*Simulation*.scala")` | Simulations filter. [See Gradle docs](https://gatling.io/docs/current/extensions/gradle_plugin/?highlight=gradle%20plugin#id2) for details. |

How to override Gatling version, JVM arguments and system properties:

```groovy
gatling {
  gatlingVersion = '3.7.0'
  jvmArgs = ['-server', '-Xms512M', '-Xmx512M']
  systemProperties = ['file.encoding': 'UTF-8']
}
```

How to filter simulations:

```groovy
gatling {
  simulations = {
    include "**/package1/*Simu.scala" // <1>
    include "**/package2/*Simulation.scala" // <2>
  }
}
```

1. all Scala files from plugin simulation dir subfolder `package1` ending with `Simu`.
2. all Scala files from plugin simulation dir subfolder `package2` ending with `Simulation`.

## Gatling configuration

### Override gatling.conf settings

To override Gatling's
[default parameters](https://github.com/gatling/gatling/blob/main/gatling-core/src/main/resources/gatling-defaults.conf),
put your own version of `gatling.conf` into `src/gatling/resources`.

### Logging management

Gatling uses [Logback](http://logback.qos.ch/documentation.html). To change the logging behaviour, put your
custom `logback.xml` configuration file in the resources folder, `src/gatling/resources`.

If no custom `logback.xml` file is provided, by default the plugin will implicitly use the following configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
      <immediateFlush>false</immediateFlush>
    </encoder>
  </appender>
  <root level="${logLevel}"> <!--1-->
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
```

1. `logLevel` is configured via plugin extension, `WARN` by default.

In case `logHttp` is configured (except for `'NONE'`), the generated `logback.xml` will look like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
      <immediateFlush>false</immediateFlush>
    </encoder>
  </appender>
  <logger name="io.gatling.http.engine.response" level="${logHttp}"/> <!--1-->
  <root level="${logLevel}"> <!--2-->
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
```

1. `logHttp` is configured via plugin extension, `TRACE` for `ALL` value and `DEBUG` for `FAILURES`
2. `logLevel` is configured via plugin extension, `WARN` by default.

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
to run particular simulations, or use the default tasks:

| Task name | Type | Description |
| --- | --- | --- |
| `gatlingClasses`           | ---            | Compiles Gatling simulation and copies resources |
| `gatlingRun`               | GatlingRunTask | Executes all Gatling simulations configured by extension |
| `gatlingRun-SimulationFQN` | GatlingRunTask | Executes single Gatling simulation\n`SimulationFQN` should be replaced by fully qualified simulation class name. |

For example, run all simulations:

```shell
gradle gatlingRun
```

Run a single simulation by its FQN (fully qualified class name):

```console
gradle gatlingRun-com.project.simu.MySimulation
```

The following configuration options are available. Those options are similar to
global `gatling` configurations. Options are used in a fallback manner, i.e. if
an option is not set the value from the `gatling` global config is taken.

| Property name | Type | Default value | Description |
| --- | --- | --- | --- |
| `jvmArgs`          | List<String>        | `null` | Additional arguments passed to JVM when executing Gatling simulations |
| `systemProperties` | Map<String, Object> | `null` | Additional systems properties passed to JVM together with caller JVM system properties |
| `simulations`      | Closure             | `null` | [See Gradle docs](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/util/PatternFilterable.html) for details. |

### Working with Gatling Enterprise Cloud

#### API tokens

You need to configure an [an API token](https://gatling.io/docs/enterprise/cloud/reference/admin/api_tokens/) for most of the tasks regarding Gatling Enterprise Cloud. The API token needs the `Configure` role.

Since you probably don’t want to include you secret token in your source code, you can configure it using either:

- the `GATLING_ENTERPRISE_API_TOKEN` environment variable
- the `gatling.enterprise.apiToken` [Java System property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)

If really needed, you can also configure it in your build.gradle:
```groovy
gatling {
  enterprise {
    apiToken "YOUR_API_TOKEN"
  }
}
```

#### Create or start a simulation

You can, using the `GatlingEnterpriseStart` task:

- configure a new simulation on Gatling Enterprise Cloud, upload your packaged code, and immediately start the simulation
- or, for a simulation already configured on Gatling Enterprise Cloud, upload any updated code and immediately start the simulation

{{< alert warning >}}
You will need to configure [an API token]({{< ref "#working-with-gatling-enterprise-cloud" >}}) with the `Configure` role.
{{< /alert >}}

Quick usage:

- configure and start a new simulation with `gradle gatlingEnterpriseStart`, you will be prompted to choose all required
  options. This will also print the simulationId of the newly configured simulation.
- run the simulation again with `gradle gatlingEnterpriseStart -Dgatling.enterprise.simulationId=<YOUR_SIMULATION_ID>`.


List of configurations used by this task:

```groovy
gatling {
  enterprise {
    // Simulation that needs to be started, you will be able to create a new simulation if empty, you can also use the gatling.enterprise.simulationId system property
    simulationId "YOUR_SIMULATION_ID"
    // default package when creating a new simulation, you can also use the gatling.enterprise.packageId system property
    packageId "YOUR_PACKAGE_ID"
    // default team when creating a new simulation, you can also use the gatling.enterprise.teamId system property
    teamId "YOUR_TEAM_ID"
    // default simulation fully qualified classname used when creating a new simulation, you can also use the gatling.enterprise.simulationClass system property
    simulationClass "computerdatabase.BasicSimulation"
    // custom system properties used when running the simulation on Gatling Enterprise
    systemProps ["KEY_1": "VALUE_1", "KEY_2": "VALUE_2"]
    //set to true if you don't want any user input, eg in a CI environment, you can also use the gatling.enterprise.batchMode system property
    batchMode false 
  }
}
```

You can run it with the command:
```shell
gradle gatlingEnterpriseStart
```

If a `simulationId` is set, the task will start the simulation on Gatling Enterprise.

If no simulationId is set, the task will ask you if you want to start or create a new simulation. If you choose create, you will be able to configure a new simulation (with the configured `packageId`, `teamId`, `simulationClass` as default), then start it. If you choose start, you will be able to start an already existing simulation on Gatling Enterprise.

If you are on a CI environment, you don't want to handle interaction with the plugin. You should then set the `batchMode` option to true. In batch mode, no input will be asked from the user, the new simulation will be created using only the configuration.

{{< alert tip >}}
Lifecycle logs need to be enabled in interactive mode.
{{< /alert >}}


#### Package

Use the task `GatlingEnterprisePackageTask` to package your simulation for Gatling Enterprise Cloud:

```shell
gradle gatlingEnterprisePackage
```

This will generate the `build/libs/<artifactId>-<version>-tests.jar` package which you can then
[upload to the Cloud](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/).

#### Package and upload

You can also create and upload the package in a single command, using the `GatlingEnterpriseUploadTask`.

{{< alert warning >}}
You will need to configure [an API token]({{< ref "#working-with-gatling-enterprise-cloud" >}}) with the `Configure` role.
{{< /alert >}}

You must already have [configured a package](https://gatling.io/docs/enterprise/cloud/reference/user/package_conf/). Copy the package ID from
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

Then package and upload your simulation to gatling Enterprise Cloud:

```shell
gradle gatlingEnterpriseUpload
```


### Working with Gatling Enterprise Self-Hosted

#### Build from sources

Once you have configured the Gradle plugin on your project, Gatling Enterprise Self-Hosted can build it from sources
without additional configuration.
[Add your source repository](https://gatling.io/docs/enterprise/self-hosted/reference/current/user/repositories/#sources-repository)
and configure your simulation to
[build from sources](https://gatling.io/docs/enterprise/self-hosted/reference/current/user/simulations/#option-1-build-from-sources)
using Gradle or Gradle Wrapper.

To make sure your setup is correct, you can run the packaging command and check that you get a jar containing all the
classes and extra dependencies of your project in `build/libs/<artifactId>-<version>-tests.jar`:

```shell
gradle gatlingEnterprisePackage
```

#### Publish to a binary repository

Alternatively, you can package your simulations and publish them to a binary repository (JFrog Artifactory, Sonatype
Nexus or AWS S3).

{{< alert tip >}}
We use the official Maven Publish plugin for Gradle; please refer to the [official documentation](https://docs.gradle.org/current/userguide/publishing_maven.html)
for generic configuration options. Please also check the standards within your organization for the best way to configure
the credentials needed to access your binary repository.
{{< /alert >}}

Configure the `maven-publish` plugin to use the task named `gatlingEnterprisePackage`, then define the repository to
publish to:

```groovy
plugins {
  id "maven-publish"
}

publishing {
  publications {
    mavenJava(MavenPublication) {
      artifact gatlingEnterprisePackage
    }
  }
  repositories {
    maven {
      if (project.version.endsWith("-SNAPSHOT")) {
        url "REPLACE_WITH_YOUR_SNAPSHOTS_REPOSITORY_URL"
      } else {
        url "REPLACE_WITH_YOUR_RELEASES_REPOSITORY_URL"
      }
    }
  }
}
```


The packaged artifact will be deployed with the `tests` classifier when you publish it:

```shell
gradle publish
```

## Troubleshooting and known issues

### Spring Boot and Netty version

[Original issue](https://github.com/lkishalmi/gradle-gatling-plugin/issues/53)

Caused by `io.spring.dependency-management` plugin and Spring platform BOM files.
The dependency management plugin ensures that all declared dependencies have
exactly the same versions as declared in BOM. Since Spring Boot declares own
Netty version (e.g. `4.1.22.Final`) - this version is applied globally for all
the configurations of the Gradle project, even if configuration does not use
Spring.

There are 2 ways of solving the problem, depending on the actual usage of Netty in the project.

* When production code does not rely on `Netty`:

`build.gradle`:
 
```groovy
ext['netty.version'] = '4.0.51.Final'
```

This declares Netty version globally for all transitive dependencies in your project, including Spring.

* When production code uses `Netty`:

`build.gradle`:

```groovy
dependencyManagement {
  gatling {
    dependencies {
      dependencySet(group: 'io.netty', version: '4.0.51.Final') {
         entry 'netty-codec-http'
         entry 'netty-codec'
         entry 'netty-handler'
         entry 'netty-buffer'
         entry 'netty-transport'
         entry 'netty-common'
         entry 'netty-transport-native-epoll'
      }
    }
  }
}
```

These options ensure that `4.0.51.Final` will be used only for `gatling` configurations, leaving other dependencies unchanged.

## Sources

If you're interested in contributing, you can find the [io.gatling.gradle plugin sources](https://github.com/gatling/gatling-gradle-plugin) on GitHub.
