---
title: "FAQ"
description: "Common questions and answers"
lead: "Common questions and answers"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-08-08T23:00:00+02:00
toc: false
weight: 2100200
---

If you are here, chances are that Gatling does not work as expected.
To help you fixing the problem, here is a list of common problems and their solutions.

If you can't find a solution here, consider joining our [Gatling Community Forum](https://community.gatling.io).

#### Why is gatling-highcharts a dedicated project/repository and why does it uses a different license?

Highcharts and Highstock are javascript libraries whose license is not open-source friendly.
We pay license fees so that we can package and distribute them and let people use them **for free**, but this module can't be open sourced.

We really want to keep as much code as possible under Apache 2, so we move the reports generation library implementation into a separate project [https://github.com/gatling/gatling-highcharts](https://github.com/gatling/gatling-highcharts).

If anyone can come with an Apache 2 licensed solution that's as sexy and plug-and-play as Highcharts and Highstock, we'd gladly make it the default implementation and integrate it into the main project!

See [License section]({{< ref "licenses" >}})

#### What is the format of the log file Gatling generates

This file is an implementation detail and is subject to change any time without any further notice.
We strongly recommend against writing your own parser and parse it for your own needs.

#### I get a "StackOverflowError" when compiling

Scenarios use method chaining **a lot**.
The longer the chain, the bigger the stack size required by the compiler to compile them.

This parameter can be increased with the `-Xss` JVM parameter. Depending on your tool, you might have to tune maven, gradle or sbt?

Another solution is to split into smaller chains.

#### I get a "Method too large" compile error

In Java and Scala, there's a method size limit. Here, the method is your Simulation constructor.

Typically, you have to move your chains out of your Simulation class, for example into objects:

{{< include-code "FaqSample.scala#chains" scala >}}

#### How can I override the maven-gatling-plugin log level?

* either set a JVM param `-Dlogback.configurationFile=/path/to/config.xml`
* or add a `logback-test.xml` to your classpath that will have precedence over the embedded `logback.xml` file

#### I don't get the number of HTTP requests I expect?

Are you sure that some requests are not being cached?
Gatling does its best to simulate real users' behavior, so HTTP caching is enabled by default.

Depending on your use case, you might either realize that the number of requests is actually perfectly fine, or you might want to [disable caching]({{< ref "../../http/protocol#caching" >}}).

#### Can Gatling launch several simulations sequentially?

No.
However, just like scheduling, that's something very easy to achieve outside Gatling.
For example, one can configure [multiple executions](http://maven.apache.org/guides/mini/guide-default-execution-ids.html) of the Gatling maven plugin, or multiple Jenkins jobs.

