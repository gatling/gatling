# What is Gatling ?

Gatling is a stress tool.
Development is currently focusing on HTTP support.

# Motivation

* Fed up with fancy GUI that generate huge unreadable XML scenarios, what you want is scripts and a user friendly DSL?
* Fed up with having to host a farm of injecting servers because your tool use blocking IO and one-thread-per-user architecture?

Gatling is for you!

# Underlying technologies

Gatling is developed in Scala and built upon :

* [Async Http Client](https://github.com/sonatype/async-http-client) and [Netty](http://netty.io) for non blocking HTTP
* [Akka](http://akka.io) for actions (requests, pauses, assertions, etc...) modeling and orchestration
* [Scalate](http://scalate.fusesource.org) for templating
...


# Release

See [changelog](https://github.com/excilys/gatling/wiki/Changelog)

# Questions, help?

Read the documentation on the [Wiki](https://github.com/excilys/gatling/wiki).

Join the [Gatling User Group](https://groups.google.com/group/gatling).

Got a real problem? Raise an [issue](https://github.com/excilys/gatling/issues?sort=created&direction=desc&state=open).

# Sponsors

[![eBusiness Information](https://github.com/excilys/gatling/wiki/img/ebi_logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)&nbsp;&nbsp;&nbsp;&nbsp;
[![Yourkit](https://github.com/excilys/gatling/wiki/img/yourkit_logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)&nbsp;&nbsp;&nbsp;&nbsp;
[![Highsoft AS](https://github.com/excilys/gatling/wiki/img/highsoft_logo.png)](https://github.com/excilys/gatling/wiki/Sponsors)

YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).

