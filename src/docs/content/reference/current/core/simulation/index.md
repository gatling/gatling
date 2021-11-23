---
title: "Simulation"
description: "Learn about the structure of the Gatling simulation"
lead: "Learn about the main parts of a Gatling simulation: DSL imports, scenario definitions, simulation definitions, hooks"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 2030200
---

`Simulation` is the parent class your tests must extend so Gatling can launch them.

{{< alert warning >}}
We recommend that your Simulation's name does not start with `Test`.
Some tools such as maven surefire aggressively consider that all classes with such naming patterns are for them to handle and they will try to launch them.
{{< /alert >}}

## DSL imports

The Gatling DSL requires some imports:

{{< include-code "imports" java kt scala >}}

{{< alert warning >}}
Do not try to "optimize imports" with your IDE, you'd break everything.
Just copy-paste those imports wherever you want to use Gatling DSL.
{{< /alert >}}

{{< alert warning >}}
Beware that any class that doesn't belong to those packages is considered private, not an API, and hence subject to change at any time without notice.
{{< /alert >}}

## setUp

Most pieces of your tests can possibly be extracted into other helper classes so you can bring your own test libraries: scenarios, protocols, headers, injection profiles, etc.

The only mandatory piece in your Simulations is that they must call the `setUp` method exactly once in their constructor to register the test components.

{{< include-code "setUp" java kt scala >}}

which correspond to injecting one single user into the `scn` scenario.

It's possible to have multiple populations, ie scenarios with an associated injection profile, in the same simulation:

{{< include-code "setUp-multiple" java kt scala >}}

For more information regarding scenarios, see the dedicated section [here]({{< ref "../scenario" >}}).

For more information regarding injection profiles, see the dedicated section [here]({{< ref "../injection" >}}).

## Protocols Configuration

Protocols configurations can be attached
* either on the setUp, in which case they are applied on all the populations
* or on each population, so they can have different configurations

{{< include-code "protocols" java kt scala >}}

For more information regarding protocols configurations, see the HttpProtocol section [here]({{< ref "../../http/protocol" >}}).

## Acceptance Criteria

Assertions are configured on the setUp.

{{< include-code "assertions" java kt scala >}}

For more information regarding assertions, see the dedicated section [here]({{< ref "../assertions" >}}).

## Global Pause configuration

The pauses can be configured on `Simulation` with a bunch of methods:

{{< include-code "pauses" java kt scala >}}

## Shaping Throughput

Some users might want to reason in terms of throughput/requests per second instead of virtual users.

If your virtual users perform only one request each, you should use an [open workload model]({{< ref "../injection/#open-model" >}}) for this, such as `constantUsersPerSec`.

Otherwise, in our opinion, you're going to run into trouble because you won't have any means of controlling which request gets executed.

Still, you can try and use the `throttle` method, that can be defined either globally or [per scenario]({{< ref "../scenario#throttling" >}}).

What `throttle` do is that it sets an upper limit on your throughput that would normally be generated with the scenarios and the injection profiles you've defined so that you can shape it the way you want. **It's only capable of reducing the normal throughput, not increase it.**

Throttling is currently only supported for HTTP requests and JMS.

{{< alert tip >}}
* Gatling will automatically interrupt your test at the end of the throttle, just like it does with [`maxDuration`]({{< ref "#maximum-duration" >}}).
* Beware that all excess traffic gets pushed into an unbounded queue, possibly resulting in an OutOfMemoryError if your normal throughput is way higher than the normal one.
* Enabling `throttle` disables `pause`s so that it can take over throughput definition.
{{< /alert >}}

{{< include-code "throttling" java kt scala >}}

This simulation will reach 100 req/s with a ramp of 10 seconds, then hold this throughput for 1 minute, jump to 50 req/s and finally hold this throughput for 2 hours.

The building blocks for throttling are:

* `reachRps(target).in(duration)`: target a throughput with a ramp over a given duration.
* `jumpToRps(target)`: jump immediately to a given targeted throughput.
* `holdFor(duration)`: hold the current throughput for a given duration.

{{< alert tip >}}
`in` is a reserved keyword in Kotlin.
You can either protect it with backticks `` `in` `` or use the `during` alias instead.
{{< /alert >}}

## Maximum Duration

Finally, with `maxDuration` you can force your run to terminate based on a duration limit, even if some virtual users are still running.

It is useful if you need to bound the duration of your simulation when you can't predict it.

{{< include-code "max-duration" java kt scala >}}

## Hooks

Gatling provides two hooks:

* `before` for executing some arbitrary code before the simulation actually runs
* `after` for executing some arbitrary code after the simulation actually runs

The lifecycle is as follows:

1. Gatling starts
2. Simulation constructor is called and all the code in the class body not delayed in `before` and `after` hooks is executed
3. `before` hook is executed
4. Simulation runs
5. Simulation terminates
6. `after` hook is executed
7. HTML reports are generated if enabled
8. Gatling shuts down

{{< include-code "hooks" java kt scala >}}

{{< alert tip >}}
You won't be able to use Gatling DSL in there, as it's only intended for load test. You can only use your own code.
If you're looking for executing Gatling DSL, you might consider using [sequential scenarios]({{< ref "../injection#sequential-scenarios" >}}).
{{< /alert >}}
