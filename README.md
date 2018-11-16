# Gatling [![Build Status](https://travis-ci.org/gatling/gatling.svg?branch=master)](https://travis-ci.org/gatling/gatling) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.gatling/gatling-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.gatling/gatling-core/)

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


## Snapshots

For people wanting to use the lastest evolutions, the SNAPSHOT versions are available from the Sonatype OSS [repository](https://oss.sonatype.org/content/repositories/snapshots/io/gatling/highcharts/gatling-charts-highcharts/).


## Questions, help?

Read the [documentation](http://gatling.io/documentation/).

Join the [Gatling User Group](https://groups.google.com/group/gatling).

Found a real bug? Raise an [issue](https://github.com/gatling/gatling/issues?sort=created&direction=desc&state=open).

## Partners

<img alt="Takima" src="https://raw.githubusercontent.com/gatling/gatling/master/src/sphinx/project/img/logo-takima-1-nom-bas.png" width="80">&nbsp;&nbsp;&nbsp;&nbsp;
![Highsoft AS](https://raw.githubusercontent.com/gatling/gatling/master/src/sphinx/project/img/highsoft_logo.png)&nbsp;&nbsp;&nbsp;&nbsp;
