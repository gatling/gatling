---
title: Project Setup
seotitle: Gatling MQTT protocol reference - project setup
description: How to set up your project to use the Gatling MQTT protocol.
lead: Learn about how to set up your project to use the Gatling MQTT protocol.
date: 2024-02-07T14:18:28+00:00
lastmod: 2024-02-07T14:18:28+00:00
---

{{< alert warning >}}
The MQTT protocol is not supported by Gatling JS.
{{< /alert >}}

## License and limitations {#license}

**The Gatling MQTT component is distributed under the
[Gatling Enterprise Component License]({{< ref "/project/licenses/enterprise-component" >}}).**

The Gatling MQTT protocol can be used with both the [Open Source](https://gatling.io/products) and
[Enterprise](https://gatling.io/products) versions of Gatling.

Its usage is unlimited when running on [Gatling Enterprise](https://gatling.io/products). When used with
[Gatling Open Source](https://gatling.io/products), usage is limited to:

- 5 users max
- 5 minute duration tests

Limits after which the test will stop.

## Getting started with the demo project {#demo-project}

A [demo project](https://github.com/gatling/gatling-mqtt-demo) is available with most combinations of currently
supported languages and build tools:

- [Java with Gradle](https://github.com/gatling/gatling-mqtt-demo/tree/main/java/gradle)
- [Java with Maven](https://github.com/gatling/gatling-mqtt-demo/tree/main/java/maven)
- [Kotlin with Gradle](https://github.com/gatling/gatling-mqtt-demo/tree/main/kotlin/gradle)
- [Kotlin with Maven](https://github.com/gatling/gatling-mqtt-demo/tree/main/kotlin/maven)
- [Scala with SBT](https://github.com/gatling/gatling-mqtt-demo/tree/main/scala/sbt)

## Adding the Gatling MQTT dependency {#gatling-mqtt-dependency}

The Gatling MQTT plugin is not included with Gatling by default. Add the Gatling MQTT dependency, in addition to the
usual Gatling dependencies.

For Java or Kotlin:

{{< include-file >}}
1-Maven: includes/dependency.maven.java.md
2-Gradle: includes/dependency.gradle.java.md
{{< /include-file >}}

For Scala:

{{< include-file >}}
1-Maven: includes/dependency.maven.scala.md
2-Gradle: includes/dependency.gradle.scala.md
3-sbt: includes/dependency.sbt.scala.md
{{< /include-file >}}
