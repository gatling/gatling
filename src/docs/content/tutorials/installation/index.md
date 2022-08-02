---
title: "Installation"
description: "Install and Gatling"
lead: "Learn about the Java, Kotlin and Scala requirements, install Gatling with the Bundle or build tool: maven, sbt, gradle "
date: 2021-04-20T18:58:06+02:00
lastmod: 2022-04-26T17:00:00+02:00
weight: 1010000
---

## Prerequisites

### Java Version

Gatling supports 64bits OpenJDK LTS (Long Term Support) versions: 8, 11 and 17.
Other JVMs such as JDK 12, client JVMs, 32bits systems or OpenJ9 are not supported.

### Supported Languages

Gatling 3.7 supports writing tests in Java, Kotlin and Scala.
Older Gatling versions only support Scala.

### Gatling Version

Make sure to use the latest version as documented on [our website](https://gatling.io/open-source/).

In particular, don't use milestones (M versions) you could find on maven central,
those are not documented and released only for internal use or [Gatling Enterprise](https://gatling.io/enterprise/) customers.

## Using the Bundle

You can use Gatling as a standalone bundle.
Then, you'll just need a text editor, possibly with Java, Kotlin or Scala syntactic coloration, to edit your simulations, and you'll be able to launch Gatling from the command line.

Check out our [Open Source page](https://gatling.io/open-source/) for the download link.

{{< alert warning >}}
The bundle only supports Java and Scala, not Kotlin. For using Kotlin, you'll need a [maven]({{< ref "#maven" >}}) or [gradle]({{< ref "#gradle" >}}) project.
{{</ alert >}}

Unzip the downloaded bundle to a folder of your choice.
Use the scripts located in the `bin` directory for launching Gatling and the Recorder.

{{< alert warning >}}
Windows users : we recommend that you do not place Gatling in the *Programs* folder as there may be permission and path issues.
{{< /alert >}}

In order to run Gatling, you need to have a JDK installed.
Gatling requires at least **JDK8**. Then, we recommend that you use an up-to-date version.

For all details regarding the installation and the tuning of the operating system (OS), please refer to the [operations]({{< ref "../../reference/current/core/operations" >}}) section.

{{< alert warning >}}
Gatling launch scripts and Gatling maven plugin honor `JAVA_HOME` env var if it's set.
Depending on your setup, you might end up running a different version than the one displayed with `java -version`.
If you get strange errors such as `Unsupported major.minor version 51.0` and you were expecting to run a JDK8 or newer, you might want to explicitly set the `JAVA_HOME` env variable.
{{< /alert >}}

The bundle structure is as follows:

* `bin`: launch scripts for Gatling and the Recorder.
* `conf`: configuration files for Gatling, Akka and Logback.
* `lib`: Gatling and dependencies binaries
* `user-files`:
    * `simulations`: where to place your Simulations code. You must respect the package folder hierarchy.
    * `resources`: non source code files such as feeder files and templates for request bodies.
    * `lib`: you can add you own dependencies (JAR files) here. When running on Gatling Enterprise, they are packaged in a single "fat JAR" file together with your Simulations code.
* `results`: where test results are generated.

## Using a Build Tool

### Maven

Maven can be used for Gatling projects with Java, Kotlin and Scala.

Gatling provides an official maven plugin named `gatling-maven-plugin`. This plugin lets you compile your Scala code and launch Gatling simulations.

Check the [maven plugin documentation]({{< ref "../../reference/current/extensions/maven_plugin" >}}) for more information.

### Gradle

Gradle can be used for Gatling projects with Java, Kotlin and Scala.

Gatling provides an official gradle plugin named `io.gatling.gradle`. This plugin lets you launch your Gatling simulations.

Check the [gradle plugin documentation]({{< ref "../../reference/current/extensions/gradle_plugin" >}}) for more information.

### Sbt

Maven can be used for Gatling projects with Scala only.

Gatling provides an official sbt plugin named `gatling-sbt`. This plugin lets you launch your Gatling simulations.

Check the [sbt plugin documentation]({{< ref "../../reference/current/extensions/sbt_plugin" >}}) for more information.

## Using an IDE

You can edit your Simulation classes with any text editor, maybe with some syntactic coloration for your chosen language.
But if you are a developer, you'll most likely want to use your favorite IDE with Gatling.

### IntelliJ IDEA

IntelliJ IDEA Community Edition comes with Java, Kotlin, maven and gradle support enabled by default.

If you want to use Scala and possibly sbt, you'll have to install the Scala plugin, which is available in the Community Edition.
You'll most likely have to increase the stack size for the scala compiler, so you don't suffer from StackOverflowErrors.
We recommend setting `Xss` to `100M`.

{{< img src="intellij-scalac-xss.png" alt="intellij-scalac-xss.png" >}}

### VS Code

We recommend that you have a look at the official documentation for setting up VS Code:
* [with Java](https://code.visualstudio.com/docs/java/java-build)
* [with Kotlin](https://kotlinlang.org/docs/jvm-get-started.html)
* [with Scala](https://scalameta.org/metals/)

### Launching Gatling and the Recorder from the IDE

All maven, gradle and sbt demo projects contain some helper classes you can use to trigger some Gatling tasks.

You can right-click on the `Engine` class in your IDE and launch the Gatling load test engine.
Simulation reports will be written in your `target` directory.

You can right-click on the `Recorder` class in your IDE and launch the Recorder.
Simulations will be generated in you `src/test/sources` directory.
