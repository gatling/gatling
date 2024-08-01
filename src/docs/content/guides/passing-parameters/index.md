---
menutitle: Passing parameters
title: Passing parameters to a simulation
seotitle: Passing parameters to a Gatling simulation
description: Use Java system properties to pass parameters to a Gatling Simulation.
lead: Use Java system properties to pass parameters to a Gatling Simulation.
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

You might want to pass parameters from the command line to the Simulation, for example the number of users, the duration of the ramp, etc.

## In Java, Kotlin or Scala

One way is to pass [Java System Properties](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html).

The way to pass such System Properties would depend on your launcher:
* maven: `mvn gatling:test -Dusers=500 -Dramp=3600`
* gradle: `gradle gatlingRun -Dusers=500 -Dramp=3600`
* sbt: `sbt -Dusers=500 -Dramp=3600 Gatling/test`

You can then resolve those properties directly in your code:

{{< include-code "injection-from-props" java kt scala >}}

## In JavaScript or TypeScript {#javascript}

The Gatling command-line tool allows you to pass options to your simulation using a `key=value` format:

```shell
npx gatling run users=500 ramp=3600
```

You can resolve these options directly in your code with the `getParameter` function:

{{< include-code "injection-from-options" js >}}

We also provide a `getEnvironmentVariable` function to read environment variables:

{{< include-code "injection-from-env-vars" js >}}
