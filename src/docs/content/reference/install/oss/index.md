---
menutitle: Gatling
title: Gatling installation
seotitle: Gatling installation with the bundle or a build tool
description: "How to install Gatling: prerequisites, different available distributions including the bundle, Maven, Gradle and sbt, and IDE integration for IntelliJ idea and Visual Studio Code (VSCode)."
lead: "Learn about the Java, Kotlin and Scala requirements, install Gatling with the Bundle or build tool: Maven, sbt, Gradle."
date: 2021-04-20T18:58:06+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

## Prerequisites

### Java Version

Gatling supports 64bits OpenJDK LTS (Long Term Support) versions: 11, 17 and 21.
Other JVMs such as 32bits systems or OpenJ9 are not supported.

### Supported Languages

Since 3.7, Gatling supports writing tests in Java, Kotlin and Scala.
Older Gatling versions only support Scala.

### Gatling version

{{< alert info >}}
Make sure to use the latest version of Gatling: `{{< var gatlingVersion >}}`.
{{< /alert >}}

In particular, don't use milestones (M versions) you could find on maven central,
those are not documented and released only for internal use or [Gatling Enterprise](https://gatling.io/products/) customers.

## Using the Bundle

You can use Gatling as a standalone bundle.
Then, you'll just need a text editor, possibly with Java or Scala syntactic coloration, to edit your simulations, and you'll be able to launch Gatling from the command line. From Gatling 3.11 the bundle is based on a Maven wrapper, and we recommend using it with and IDE such as IntelliJ. 

To install the bundle, download and extract the following `ZIP` file:

{{< button title="Download Gatling" >}}
https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/{{< var gatlingVersion >}}/gatling-charts-highcharts-bundle-{{< var gatlingVersion >}}.zip
{{< /button >}}

{{< alert warning >}}
The bundle only supports Java and Scala, not Kotlin. To use Kotlin, you'll need a [Maven]({{< ref "#maven" >}}) or [Gradle]({{< ref "#gradle" >}}) project.
{{</ alert >}}


{{< alert warning >}}
Windows users: we recommend that you do not place Gatling in the *Programs* folder as there may be permission and path issues.
{{< /alert >}}

In order to run Gatling, you need to have a JDK installed.
Gatling requires at least **JDK11**. Then, we recommend that you use an up-to-date version.

For all details regarding the installation and the tuning of the operating system (OS), please refer to the [operations]({{< ref "../script/core/operations" >}}) section.

{{< alert warning >}}
Gatling launch scripts and Gatling Maven plugin honor `JAVA_HOME` env var if it's set.
Depending on your setup, you might end up running a different version than the one displayed with `java -version`.
If you get strange errors such as `Unsupported major.minor version` and you were expecting to run a JDK11 or newer, you might want to explicitly set the `JAVA_HOME` env variable.
{{< /alert >}}

The bundle structure is as follows:

* `src/test/java`: where to place your Simulations code. You must respect the package folder hierarchy.
* `src/test/resources`: non source code files such as feeder files and templates for request bodies and configuration files for Gatling, Akka and Logback.
* `pom.xml`: Maven informations about the project.
* `target`: where test results are generated.

### Run a Gatling simulation

Use the following command to starts Gatling in interactive mode:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:test
Windows: mvnw.cmd gatling:test
{{</ code-toggle >}}

### Start the Gatling Recorder

The [Gatling Recorder]({{< ref "/reference/script/protocols/http/recorder/" >}}) allows you to capture browser-based actions and convert them into a script. Use the following command to launch the Recorder:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:recorder
Windows: mvnw.cmd gatling:recorder
{{</ code-toggle >}}


## Using a Build Tool

### Maven

Maven can be used for Gatling projects with Java, Kotlin and Scala.

Gatling provides an official maven plugin named `gatling-maven-plugin`. This plugin lets you compile your code and launch Gatling simulations.

Check the [Maven plugin documentation]({{< ref "../extensions/build-tools/maven-plugin" >}}) for more information.

### Gradle

Gradle can be used for Gatling projects with Java, Kotlin and Scala.

Gatling provides an official gradle plugin named `io.gatling.gradle`. This plugin lets you launch your Gatling simulations.

Check the [Gradle plugin documentation]({{< ref "../extensions/build-tools/gradle-plugin" >}}) for more information.

### sbt

sbt can be used for Gatling projects with Scala only.

Gatling provides an official sbt plugin named `gatling-sbt`. This plugin lets you launch your Gatling simulations.

Check the [sbt plugin documentation]({{< ref "../extensions/build-tools/sbt-plugin" >}}) for more information.

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

All Maven, Gradle and sbt demo projects contain some helper classes you can use to trigger some Gatling tasks.

You can right-click on the `Engine` class in your IDE and launch the Gatling load test engine.
Simulation reports will be written in your `target` directory.

You can right-click on the `Recorder` class in your IDE and launch the Recorder.
Simulations will be generated in you `src/test/sources` directory.
