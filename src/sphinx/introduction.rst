.. _introduction:

############
Introduction
############

What is Gatling?
================

Gatling is a mainly stress tool: a tool used to generate populations of virtual users that will browse an application and generate traffic.

Typical goals are:

  * Anticipating problems on the live environment before they actually happen and damage business
  * Reproducing problems that happens on the live environment and investing them

Thanks to the `Assertion API <general/assertions.html>`_, Gatling is also suited functional tests, typically on Web API, thus mutualizing the functional and performance testing efforts.

Gatling's main focus is the HTTP protocol, but it can be extended so support other protocols as well.

Why Gatling?
============

Gatling was born from the following requirements:

  * scenarios that are real code:

    * a `DSL <http://en.wikipedia.org/wiki/Domain-specific_language>`_ is more self explanatory than an over bloated Graphical User Interface.
    * code is more convenient for developers.
    * have one single way of doing things: out-of-the-box features and user custom hacks use the same technology.
    * code is easier to maintain and can be injected into a VCS, on contrary to a huge GUI XML dump.

  * resource efficient, scaling out should be a last resort, once saturating the NIC.
  * run on a JVM, a widely spread execution platform that ops are used to administrate.


Technical Background
====================

Gatling runs on a JVM, preferably on an up-to-date JDK7.

Gatling's architecture is asynchronous as long as the underlying protocol, such as HTTP, can be implemented in a non blocking way.

This kind of architecture let us implement virtual users as messages instead of dedicated threads, making them very cheap.
Thus, running thousands of concurrent virtual users is not an issue.

Gatling's core orchestration engine is base one the `Actor model <http://en.wikipedia.org/wiki/Actor_model>`_  and is implemented on top of `Akka <http://akka.io>`_.

Gatling's HTTP engine is based on `Java NIO <http://docs.oracle.com/javase/7/docs/api/java/nio/package-summary.html>`_ and is implemented of top of `Netty <http://netty.io>`_ and `AsyncHttpClient <https://github.com/AsyncHttpClient/async-http-client>`_.
