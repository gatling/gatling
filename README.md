# What is Gatling ?

Gatling is a stress tool.
Development is currently focusing on HTTP support.

# Motivation

* Fed up with fancy GUI that generate huge unreadable XML scenarios, what you want is scripts and a user friendly DSL?
* Fed up with having to host a farm of injecting servers because your tool use blocking IO and one-thread-per-user architecture?

Gatling is for you!

# Underlying technologies

Gatling is built upon :

* [Async Http Client](https://github.com/sonatype/async-http-client) and [Netty](http://www.jboss.org/netty) for non blocking HTTP
* Akka for actions (requests, pauses, assertions, etc...) modeling and orchestration
* Scala interpreter for scripting
...


# Status
Currently under development.
We aim for a 1.0 release by the end of november.

# Sponsors

[![eBusiness Information](https://github.com/excilys/gatling/wiki/img/ebi_logo.png) ![Highsoft AS](https://github.com/excilys/gatling/wiki/img/highsoft_logo.png) ![Yourkit](https://github.com/excilys/gatling/wiki/img/yourkit_logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)
