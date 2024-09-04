---
title: Assertions
seotitle: Gatling assertions scripting reference
description: How to use assertions to define acceptance criteria and have your test pass or failed based on response time or request status statistics.
lead: Learn how to set assertions on metrics like response time or number of failed requests, and export these results to a JUnit compatible format
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

## Concepts

The Assertions API is used to verify that global statistics, like response time or number of failed requests, match expectations for a whole simulation.

Assertions are registered for a simulation using the method `assertions` on the `setUp`. For example:

{{< include-code "setUp" >}}

This method takes as many assertions as you like.

The API provides a dedicated DSL for chaining the following steps:

1. defining the scope of the assertion
2. selecting the statistic
3. selecting the metric
4. defining the condition

All the assertions are evaluated after running the simulation. If at least one assertion fails, the simulation fails.

## Scope

An assertion can test a statistic calculated from all requests or only part of them.

* `global`: use statistics calculated from all requests.
* `forAll`: use statistics calculated for each individual request.
* `details(path)`: use statistics calculated from a group or a request. The path is defined like a Unix filesystem path.

For example, to perform an assertion on the request `MyRequest`, use:

{{< include-code "details" >}}

and to perform an assertion on the request `MyRequest` in the group `MyGroup`, use:

{{< include-code "details-group" >}}

For WebSockets it takes the name of the check and not the name of the request. `ws.checkTextMessage("use this name")`

{{< alert tip >}}
When `path` is a group, assertions are matched against the cumulated response time, not the group total duration.
For more information on the distinction between groups cumulated response time and duration, see [the Groups timings documentation]({{< ref "../../stats/timings#groups" >}}).
{{< /alert >}}

## Statistics

* `responseTime`: target the response time in milliseconds.
* `allRequests`: target the number of requests.
* `failedRequests`: target the number of failed requests.
* `successfulRequests`: target the number of successful requests.
* `requestsPerSec`: target the rate of requests per second.

## Selecting the metric

### Applicable to response time

* `min`: perform the assertion on the minimum of the metric.
* `max`: perform the assertion on the maximum of the metric.
* `mean`: perform the assertion on the mean of the metric.
* `stdDev`: perform the assertion on the standard deviation of the metric.
* `percentile1`: perform the assertion on the 1st percentile of the metric, as configured in `gatling.conf` (default is 50th).
* `percentile2`: perform the assertion on the 2nd percentile of the metric, as configured in `gatling.conf` (default is 75th).
* `percentile3`: perform the assertion on the 3rd percentile of the metric, as configured in `gatling.conf` (default is 95th).
* `percentile4`: perform the assertion on the 4th percentile of the metric, as configured in `gatling.conf` (default is 99th).
* `percentile(value: Double)`: perform the assertion on the given percentile of the metric. Parameter is a percentage, between 0 and 100.

### Applicable to number of requests (all, failed or successful)

* `percent`: use the value as a percentage between 0 and 100.
* `count`: perform the assertion directly on the count of requests.

## Condition

Conditions can be chained to apply several conditions on the same metric.

* `lt(threshold)`: check that the value of the metric is less than the threshold.
* `lte(threshold)`: check that the value of the metric is less than or equal to the threshold.
* `gt(threshold)`: check that the value of the metric is greater than the threshold.
* `gte(threshold)`: check that the value of the metric is greater than or equal to the threshold.
* `between(thresholdMin, thresholdMax)`: check that the value of the metric is between two thresholds.
* `between(thresholdMin, thresholdMax, inclusive = false)`: same as above but doesn't include bounds
* `around(value, plusOrMinus)`: check that the value of the metric is around a target value plus or minus a given margin.
* `around(value, plusOrMinus, inclusive = false)`: same as above but doesn't include bounds
* `deviatesAround(target, percentDeviationThreshold)`: check that metric is around a target value plus or minus a given relative margin
* `deviatesAround(target, percentDeviationThreshold, inclusive = false)`: same as above but doesn't include bounds
* `is(value)`: check that the value of the metric is equal to the given value.
* `in(sequence)`: check that the value of metric is in a sequence.

{{< alert tip >}}
`is` is a reserved keyword in Kotlin.
You can either protect it with backticks `` `is` `` or use the `shouldBe` alias instead.

`in` is a reserved keyword in Kotlin.
You can either protect it with backticks `` `in` `` or use the `within` alias instead.
{{< /alert >}}

## Putting It All Together

To help you understand how to use assertions, here is a list of examples:

{{< include-code "examples" >}}

{{< alert tip >}}
Gatling Enterprise includes ramp up and ramp down [time window options]({{< ref "/reference/execute/cloud/user/simulations/#step-4-time-window" >}}), which allows you to exclude warmup times from the assertions calculation. 
{{< /alert >}}
