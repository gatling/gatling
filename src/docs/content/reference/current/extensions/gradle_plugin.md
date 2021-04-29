---
title: "Gradle Plugin"
description: "Gradle plugin to run Gatling test"
lead: "The Gradle plugin allows you to run Gatling test from the command line, without the bundle"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

This Gradle plugin was initially contributed by [Ievgenii Shepeliuk](https://github.com/eshepelyuk) and [Laszlo Kishalmi](https://github.com/lkishalmi).

This Gradle plugin integrates Gatling with Gradle, allowing to use Gatling as a testing framework.

## Versions

Check out available versions on [Gradle Plugins Portal](https://plugins.gradle.org/).

## Setup

In `build.gradle`, add:

```groovy
 plugins {
   id 'io.gatling.gradle' version "MANUALLY_REPLACE_WITH_LATEST_VERSION"
 }
```

## Demo sample

You can find a [sample project demoing the io.gatling.gradle plugin](https://github.com/gatling/gatling-gradle-plugin-demo) in Gatling's Github organization.

## Compatibility

Gradle version

* Minimal supported Gradle version is 4.0.
* Maximum version is 6.6.1

Scala version

* Gatling uses Scala version 2.12 since version 3.0.0, so the plugin does.

## Installation

1. Install [Gradle](https://gradle.org/install/)
2. Create a new project directory, and a file name `build.gradle` within it
3. Follow [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.gatling.gradle) instructions

## Source files layout

Plugin creates dedicated [Gradle sourceSet](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html) named `gatling`. This source set is used for storing simulations and Gatling configurations. Following directories are configured by default.

| Directory               | Purpose                                         |
| ----------------------- | ----------------------------------------------- |
| `src/gatling/scala`     | Simulation sources (Scala code)                 |
| `src/gatling/resources` | Resources (feeders, configuration, bodies, etc) |

Using Gradle API file locations can be customized.

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
| `toolVersion`       | String  | `'3.4.0'` | Gatling version |
| `logLevel`          | String  | `'WARN'` | The default Gatling console log level if no `logback.xml` present in the configuration folder |
| `logHttp`           | String  | `'NONE'` | Verbosity of logging HTTP requests performed by Gatling, must be one of: <br/> * `'NONE'` - do not log, <br/> * `'ALL'` - log all requests, <br/> * `'FAILURES'` - only failed requests |
| `includeMainOutput` | Boolean | `true` | `true` |
| `includeTestOutput` | Boolean | `true` | Include test source set output to gatlingImplementation |
| `scalaVersion`      | String  | `'2.12.8'` | Scala version that fits your Gatling tool version |
| `jvmArgs`           | List    | <pre>[<br>  '-server',<br>  '-Xmx1G',<br>  '-XX:+HeapDumpOnOutOfMemoryError',<br>  '-XX:+UseG1GC',<br>  '-XX:+ParallelRefProcEnabled',<br>  '-XX:MaxInlineLevel=20',<br>  '-XX:MaxTrivialSize=12',<br>  '-XX:-UseBiasedLocking'<br>]</pre> | Additional arguments passed to JVM when executing Gatling simulations |
| `systemProperties`  | Map     | `['java.net.preferIPv6Addresses': true]` | Additional systems properties passed to JVM together with caller JVM system properties |
| `simulations`       | Closure | `{ include "**/*Simulation*.scala" }` | Simulations filter. [See Gradle docs](https://gatling.io/docs/current/extensions/gradle_plugin/?highlight=gradle%20plugin#id2) for details. |

How to override Gatling version, JVM arguments and system properties

```groovy
gatling {
  toolVersion = '3.4.0'
  jvmArgs = ['-server', '-Xms512M', '-Xmx512M']
  systemProperties = ['file.encoding': 'UTF-8']
}
```

How to filter simulations

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

To override
[default parameters](https://github.com/gatling/gatling/blob/master/gatling-core/src/main/resources/gatling-defaults.conf)
of Gatling just put own version of `gatling.conf` into `src/gatling/resources`.

### Logging management

Gatling uses [Logback](http://logback.qos.ch/documentation.html) to customize
its output. To change logging behaviour, put your `logback.xml` into resources
folder, `src/gatling/resources`.

If no custom `logback.xml` provided, by default plugin will implicitly use following configuration.

Default `logback.xml` created by the plugin

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

In case `logHttp` is configured (except for `'NONE'`), the generated `logback.xml` will look like:

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

This plugin defines three [Gradle configurations](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html) `gatling`, `gatlingImplementation` and `gatlingRuntimeOnly`.

By default, plugin adds Gatling libraries to `gatling` configuration.
Configurations `gatlingImplementation` and `gatlingRuntimeOnly` extend `gatling`,
i.e. all dependencies declared in `gatling` will be inherited. Dependencies added
to configurations other than these '`gatling`' configurations will not be available
within Gatling simulations.

Also, project classes (`src/main`) and tests classes (`src/test`) are added to
`gatlingImplementation` and `gatlingRuntimeOnly` classpath, so you can reuse
existing production and test code in your simulations.

If you do not need such behaviour, you can use flags:

Manage test and main output

```groovy
gatling {
  // do not include classes and resources from src/main
  includeMainOutput = false
  // do not include classes and resources from src/test
  includeTestOutput = false
}
```

Additional dependencies can be added by plugin's users to any of configurations
mentioned above.

Add external libraries for `Gatling` simulations

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

Plugin provides `GatlingRunTask` that is responsible for executing Gatling
simulations. Users may create own instances of this task to run particular
simulations.

Following configuration options are available. Those options are similar to
global `gatling` configurations. Options are used in a fallback manner, i.e. if
option is not set the value from `gatling` global config is taken.

| Property name | Type | Default value | Description |
| --- | --- | --- | --- |
| `jvmArgs`          | List<String>        | `null` | Additional arguments passed to JVM when executing Gatling simulations |
| `systemProperties` | Map<String, Object> | `null` | Additional systems properties passed to JVM together with caller JVM system properties |
| `simulations`      | Closure             | `null` | [See Gradle docs](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/util/PatternFilterable.html) for details. |

### Default tasks

| Task name | Type | Description |
| --- | --- | --- |
| `gatlingClasses`           | ---            | Compiles Gatling simulation and copies resources |
| `gatlingRun`               | GatlingRunTask | Executes all Gatling simulations configured by extension |
| `gatlingRun-SimulationFQN` | GatlingRunTask | Executes single Gatling simulation\n`SimulationFQN` should be replaced by fully qualified simulation class name. |

Run all simulations

```bash
$ gradle gatlingRun
```

Run single simulation implemented in `com.project.simu.MySimulation` class

```bash
$ gradle gatlingRun-com.project.simu.MySimulation
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

If you're interested in contributing, you can find the [io.gatling.gradle plugin sources](https://github.com/gatling/gatling-gradle-plugin) on Github.
