# Gatling [![Build Status](https://travis-ci.org/gatling/gatling.svg?branch=master)](https://travis-ci.org/gatling/gatling) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.gatling/gatling-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.gatling/gatling-core/)

## What is Gatling ?

Gatling is a load test tool.
It officially supports HTTP, WebSocket, Server-Sent-Events and JMS.

## Motivation

* Finding fancy GUIs not that convenient for describing load tests, what you want is a friendly expressive DSL?
* Wanting something more convenient than huge XML dumps to store in your source version control system?
* Fed up with having to host a farm of injecting servers because your tool uses blocking IO and one-thread-per-user architecture?

Gatling is for you!

## Underlying technologies

Gatling is developed in Scala and built upon :

* [Netty](https://netty.io) for non blocking HTTP
* [Akka](https://akka.io) for virtual users orchestration
...

## Questions, help?

Read the [documentation](https://gatling.io/docs/current/).

Join the [Gatling User Group](https://groups.google.com/forum/#!forum/gatling).

Found a real bug? Raise an [issue](https://github.com/gatling/gatling/issues).

## Partners

<img alt="Takima" src="https://raw.githubusercontent.com/gatling/gatling/master/src/sphinx/project/img/logo-takima-1-nom-bas.png" width="80">&nbsp;&nbsp;&nbsp;&nbsp;
![Highsoft AS](https://raw.githubusercontent.com/gatling/gatling/master/src/sphinx/project/img/highsoft_logo.png)&nbsp;&nbsp;&nbsp;&nbsp;
