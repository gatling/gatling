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
* Scala REPL for scripting
...


# Status
Currently under development.
We aim for a 1.0 release by the end of november.

# Tooling
YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).
