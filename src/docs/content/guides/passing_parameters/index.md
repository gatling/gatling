---
title: "Passing Parameters"
description: "Using Java options in a Gatling Simulation"
lead: "Using Java options in a Gatling Simulation"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 0031
---

You might want to pass parameters from the command line to the Simulation, for example the number of users, the duration of the ramp, etc.

One way is to pass [Java System Properties](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html).

The way to pass such System Properties would depend on your launcher:
* maven: `mvn gatling:test -Dusers=500 -Dramp=3600`
* gradle: `gradle gatlingRun -Dusers=500 -Dramp=3600`
* sbt: `sbt -Dusers=500 -Dramp=3600 Gatling/test`
* Gatling bundle's `gatling.sh` or `gatling.bat`: set the `JAVA_OPTS` env var, eg `JAVA_OPTS="-Dusers=500 -Dramp=3600"`

You can then resolve those properties directly in your code:

{{< include-code "injection-from-props" java kt scala >}}

