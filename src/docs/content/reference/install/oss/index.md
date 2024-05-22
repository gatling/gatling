---
menutitle: Gatling
title: Gatling installation
seotitle: Gatling installation with the bundle, build tool, or package manager
description: "How to install Gatling: prerequisites, different available distributions including the bundle, Maven, Gradle, sbt, and npm. Also IDE integration for IntelliJ idea and Visual Studio Code (VSCode)."
lead: "Learn how to install Gatling for Java, Kotlin, Scala, JavaScript, or TypeScript. Install Gatling with the Maven, sbt, or Gradle build tool or a JavaScript package manager."
date: 2021-04-20T18:58:06+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

## Introduction

Gatling runs on a Java Virtual Machine (JVM). You can write tests in Java, Scala, Kotlin, JavaScript, or TypeScript. We strongly recommend downloading the project `ZIP` file from this page and following the subsequent installation instructions for all languages. 

The installation requirements and procedures for JavaScript and TypeScript differ from the native JVM languages. To use Gatling with JavaScript or TypeScript, skip to the [Use a JavaScript package manager]({{< ref "#use-a-javascript-package-manager" >}}) section. 

Gatling also provides a [standalone bundled version]({{< ref "#use-the-standalone-bundle" >}}) that is intended for users behind a corporate firewall who might have restricted access to Maven (for example). 

## Prerequisites for Java, Scala, and Kotlin

### Java version

Gatling supports 64-bit OpenJDK LTS (Long Term Support) versions: 11, 17, and 21.
Other JVMs such as, 32-bit systems or OpenJ9, are not supported. We recommend the [Azul JDK](https://www.azul.com/downloads/?package=jdk#zulu).

### Supported languages

Since 3.7, Gatling supports writing tests in Java, Scala, and Kotlin.
Older Gatling versions only support Scala. 

{{< alert info >}}
We recommend selecting Java unless you are an experienced Scala or experienced Kotlin developer. 
{{< /alert >}} 

### Gatling version

{{< alert info >}}
Make sure to use the latest version of Gatling: `{{< var gatlingVersion >}}`.
{{< /alert >}}

In particular, don't use milestones (M versions) you could find on Maven Central;
those are not documented and are released only for internal use or [Gatling Enterprise](https://gatling.io/products/) customers.


## Install Gatling using a `ZIP` file for build tools

Build tools are the preferred way for Gatling users to launch and run their scripts. From Gatling 3.11, the build tool plugins have similar functionality and commands while respecting each tool's conventions. Additionally, the Maven and Gradle tools have wrappers that allow you to run Gatling without installing Maven or Gradle on your computer. The following download buttons provide links for Maven-Java and Gradle-Java projects. Installation instructions for Kotlin and Scala are located in the [Maven]({{< ref "../integrations/build-tools/maven-plugin" >}}), [Gradle]({{< ref "../integrations/build-tools/gradle-plugin" >}}), and [sbt]({{< ref "../integrations/build-tools/sbt-plugin" >}}) plugin documentation. 

If you are unsure which version to select, Java-Maven is recommended.

To install Gatling:
1. Download your preferred configuration.
2. Unzip the folder. 
3. Open the folder in your IDE.



{{< button title="Download Gatling for Maven-Java" >}}
https://github.com/gatling/gatling-maven-plugin-demo-java/archive/refs/heads/main.zip{{< /button >}}  

{{< button title="Download Gatling for Gradle-Java" >}}
https://github.com/gatling/gatling-gradle-plugin-demo-java/archive/refs/heads/main.zip
{{< /button >}}  

## Manually install and configure Gatling with a build tool 

You can install and configure Gatling manually by following instructions in the build tool plugin documentation:

- [Maven]({{< ref "../integrations/build-tools/maven-plugin" >}})
- [Gradle]({{< ref "../integrations/build-tools/gradle-plugin" >}})
- [sbt]({{< ref "../integrations/build-tools/sbt-plugin" >}}) 


## Use a JavaScript package manager

{{< alert info >}} 
- The JavaScript SDK currently only covers the `HTTP` protocol. 
- Gatling supports installation using npm. If you use another package manager such as Yarn, you need to modify the commands accordingly.
{{< /alert >}}

To install Gatling for JavaScript and TypeScript, you must have:

- [Node.js](https://nodejs.org/en/download) v18 or later (LTS versions only)
- npm v8 or later (included in the NodeJS installation)

Then, use the following procedure to install Gatling:

1. Download the Gatling JS demo project zip file using the following download button:
{{< button title="Download Gatling for JavaScript" >}}
https://github.com/gatling/gatling-js-demo/archive/refs/heads/main.zip{{< /button >}}  

2. Unzip and open the project in your IDE or terminal.
3. navigate to the `/javascript` folder for JavaScript projects or the `/typescript` folder for TypeScript projects in your terminal. 
4. Run `npm install` to install the packages and dependencies. 

You can run the pre-configured demo simulation from the `src/` folder with the following command:


{{< code-toggle console >}}
JavaScript: npx gatling run --simulation computerdatabase
TypeScript: npx gatling run --typescript --simulation computerdatabase
{{</ code-toggle >}}


{{< alert info >}}
To learn more about developing Gatling tests in JavaScript/TypeScript, follow the [Intro to scripting]({{< ref "/tutorials/scripting-intro/" >}}) tutorial.
{{< /alert >}}

## Use the standalone bundle

The Gatling bundle is primarily intended for users who don't have internet access (e.g., behind a corporate firewall). Otherwise, we strongly recommend using the Maven plugin, which is lighter and easier to push to Git. Pay attention to the subsequent warnings to understand the limitations of the standalone bundle. 

From Gatling 3.11, the bundle is based on a Maven wrapper, and we recommend using it with an IDE such as IntelliJ. 

{{< button title="Download for Gatling bundle" >}}
https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/{{< var gatlingVersion >}}/gatling-charts-highcharts-bundle-{{< var gatlingVersion >}}.zip
{{< /button >}}


{{< alert warning >}}
The bundle only supports Java and Scala, not Kotlin. To use Kotlin, you'll need [Maven or Gradle]({{< ref "#install-gatling-using-a-zip-file-for-build-tools" >}}) project.
{{</ alert >}}

{{< alert warning >}}
Windows users: 
- we recommend that you do not place Gatling in the *Programs* folder as there may be permission and path issues. 
- The standard Windows zip tool will not work to extract the bundle. We recommend using [7-zip](https://www.7-zip.org/) instead.
{{< /alert >}}

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

## Use Gatling with an IDE

You can edit your Simulation classes with any text editor, maybe with some syntactic coloration for your chosen language.
But if you are a developer, you'll most likely want to use your favorite IDE with Gatling.

### IntelliJ IDEA

IntelliJ IDEA Community Edition comes with Java, Kotlin, maven and gradle support enabled by default.

If you want to use Scala and possibly sbt, you'll have to install the Scala plugin, which is available in the Community Edition.
You'll most likely have to increase the stack size for the Scala compiler so you don't suffer from StackOverflowErrors.
We recommend setting `Xss` to `100M`.

{{< img src="intellij-scalac-xss.png" alt="intellij-scalac-xss.png" >}}

### VS Code

We recommend that you have a look at the official documentation for setting up VS Code:
* [with Java](https://code.visualstudio.com/docs/java/java-build)
* [with Kotlin](https://kotlinlang.org/docs/jvm-get-started.html)
* [with Scala](https://scalameta.org/metals/)


## Run a demo load test

If you installed Gatling using a `ZIP` file download it comes pre-loaded with a fully functioning load test. You can run this test locally to instantly experience Gatling's functionality and reports feature. The following sections help you to run your first test. The provided commands work for Maven, JavaScript/TypeScript, and the bundle. Refer to the plugin documentation for [Gradle]({{< ref "../integrations/build-tools/gradle-plugin" >}}) or [sbt]({{< ref "../integrations/build-tools/sbt-plugin" >}}) users. 

### Run a Gatling simulation

Use the following command to start Gatling in interactive mode:

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

For the JavaScript SDK:

```console
npx gatling recorder
```
