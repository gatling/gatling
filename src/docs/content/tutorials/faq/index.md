---
menutitle: FAQ
title: Gatling frequently asked questions (FAQ)
description: Common questions and answers
lead: Common questions and answers
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

Below are answers to some of the questions we receive regularly from Gatling and Gatling Enterprise users.

If you can't find a solution here or in the rest of documentation, post your question in the [Gatling Community Forum](https://community.gatling.io).


## Scripting scenarios

### Which languages can I use to write scripts? 

At present Gatling Enterprise supports the following languages:
- Java 
- Scala
- Kotlin 
- Javascript
- Typescript

We typically recommend Java because it is widely taught in Computer Science programs and makes including additional developers easier. Gatling is constantly evolving, and more SDKs will likely be added in the future.


### Can I migrate my Gatling open-source scripts to Gatling Enterprise?

Yes, the tests you develop for Gatling open source are compatible with Gatling Enterprise without requiring any modifications. If you have a Gatling Enterprise account, you can view Gatling simulations that have been migrated and are already available on the Enterprise platform.

Gatling provides plugins for Maven, Gradle, and sbt. These plugins allow for a straightforward transition of your open-source script into Gatling Enterprise.

JavaScript and TypeScript users can use their preferred package manager to create `zip` files for running tests on Gatling Enterprise.

## Running simulations

### What is the format of the log file Gatling generates?

This file is an implementation detail and is subject to change any time without any further notice.
We strongly recommend against writing your own parser and parse it for your own needs.

### I get a "StackOverflowError" when compiling

Scenarios use method chaining **a lot**.
The longer the chain, the bigger the stack size required by the compiler to compile them.

This parameter can be increased with the `-Xss` JVM parameter. Depending on your tool, you might have to tune maven, gradle or sbt?

Another solution is to split into smaller chains.

### I get a "Method too large" compile error

In Java and Scala, there's a method size limit. Here, the method is your Simulation constructor.

Typically, you have to move your chains out of your Simulation class, for example into objects:

{{< include-code "FaqSample.scala#chains" scala >}}

### How can I override the maven-gatling-plugin log level?

* either set a JVM param `-Dlogback.configurationFile=/path/to/config.xml`
* or add a `logback-test.xml` to your classpath that will have precedence over the embedded `logback.xml` file

### When should I use more load generators?

There are 3 main reasons to consider using additional load generators:

- your existing load generators are under a lot of stress,
- you want more than 50,000 concurrent virtual users,
- you want to add virtual users from different geographic locations to better mimic your real usage patterns. 

### How much load can 1 load injector generate with Gatling?

This is the most common question we receive from people interested in Gatling Enterprise.

Gatling’s simulation capacity is determined by several factors including:

- protocols, 
- resource usage, 
- user actions, and 
- script optimization. 

For example, if 1 user = 1 request, you can generate 64K users /second on your local machine. This is limited by the operating system, not by Gatling. 

Larger loads are possible with Gatling Enterprise by utilizing distributed testing and the ability to add additional load generators.

For Gatling Enterprise Cloud, we use AWS EC2 instances as load injectors, which can simulate up to 40,000 virtual users per second or the equivalent of 300,000 requests per second. However, not all requests are built equally and some may take more work from the injectors than others. To figure out how many injectors you need, we recommend starting with as few injectors as possible and checking the injector monitoring tab of your reports. You can determine if you need additional injectors based on metrics like CPU usage. 


### Can Gatling launch several simulations sequentially?

No.

However, just like scheduling, that's something very easy to achieve outside Gatling.
For example, one can configure [multiple executions](http://maven.apache.org/guides/mini/guide-default-execution-ids.html) of the Gatling maven plugin, or multiple Jenkins jobs.



## Understanding test results

### I don't get the number of HTTP requests I expect?

Are you sure that some requests are not being cached?
Gatling does its best to simulate real users' behavior, so HTTP caching is enabled by default.

Depending on your use case, you might either realize that the number of requests is actually perfectly fine, or you might want to [disable caching]({{< ref "/reference/script/protocols/http/protocol#disablecaching" >}}).

### How do I use the percentages in Gatling’s reports?

When running a Gatling simulation, you receive a detailed breakdown of response times for each tested request. This breakdown includes various percentages of response times, along with the mean or average.

While it may be tempting to focus solely on the average response time, this metric can sometimes be misleading. To illustrate, consider a test on an e-commerce site with 10 users, resulting in the following response time distribution:

- 5 users load within 2 seconds
- 2 users wait for 3 seconds
- 2 users experience a 5-second wait
- 1 user endures an 8-second wait

Calculating the average yields 3.4 seconds. However, it's crucial to note that 30% of your users are waiting over 5 seconds, indicating a significant portion facing delays beyond what might be deemed acceptable.

As traffic increases, prioritizing faster response times for users in the higher percentiles becomes essential. For instance, in a test with 10,000 users, having 1,000 of them experiencing a 10,000-millisecond load time is considered unacceptable.

To ensure a more accurate understanding of user experience, it is advisable to examine response time percentiles, especially in scenarios with varying user loads. This approach provides a more nuanced perspective on performance and helps identify potential issues that might be masked by a simple average.



## Gatling project

### Why is gatling-highcharts a dedicated project/repository and why does it use a different license?

Highcharts and Highstock are javascript libraries whose license is not open-source friendly.
We pay license fees so that we can package and distribute them and let people use them **for free**, but this module can't be open sourced.

We really want to keep as much code as possible under Apache 2, so we move the reports generation library implementation into a separate project [https://github.com/gatling/gatling-highcharts](https://github.com/gatling/gatling-highcharts).

If anyone can come with an Apache 2 licensed solution that's as sexy and plug-and-play as Highcharts and Highstock, we'd gladly make it the default implementation and integrate it into the main project!

See [License section]({{< ref "licenses" >}})


