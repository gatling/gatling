# Gatling [![Build Status](https://travis-ci.org/excilys/gatling.png?branch=master)](https://travis-ci.org/excilys/gatling)

Gatling versions 2.0.0-M3a and lower build with `maven`, master builds with `sbt`.

## What is Gatling ?

Gatling is a stress tool.
Development is currently focusing on HTTP support.

## Motivation

* Finding fancy GUIs not that convenient for describing stress tests, what you want is a friendly expressive DSL?
* Wanting something more convenient than huge XML dumps to store in your source version control system?
* Fed up with having to host a farm of injecting servers because your tool uses blocking IO and one-thread-per-user architecture?

Gatling is for you!

## Underlying technologies

Gatling is developed in Scala and built upon :

* [Async Http Client](https://github.com/AsyncHttpClient/async-http-client) and [Netty](http://netty.io) for non blocking HTTP
* [Akka](http://akka.io) for actions (requests, pauses, assertions, etc...) modeling and orchestration
...


## Release

See [changelog](https://github.com/excilys/gatling/wiki/Changelog).

For people wanting to use the lastest evolutions, the SNAPSHOT versions are available from the Sonatype OSS [repository](https://oss.sonatype.org/content/repositories/snapshots/io/gatling/highcharts/gatling-charts-highcharts/).


## Questions, help?

Read the documentation on the [Wiki](https://github.com/excilys/gatling/wiki).

Join the [Gatling User Group](https://groups.google.com/group/gatling).

Got a real problem? Raise an [issue](https://github.com/excilys/gatling/issues?sort=created&direction=desc&state=open).

## Sponsors

[![eBusiness Information](https://github.com/excilys/gatling/wiki/img/ebi_logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)&nbsp;&nbsp;&nbsp;&nbsp;
[![Yourkit](https://github.com/excilys/gatling/wiki/img/yourkit_logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)&nbsp;&nbsp;&nbsp;&nbsp;
[![Highsoft AS](https://github.com/excilys/gatling/wiki/img/highsoft_logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)&nbsp;&nbsp;&nbsp;&nbsp;
[![Cloudbees](https://github.com/excilys/gatling/wiki/img/devcloud-logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)

YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).

