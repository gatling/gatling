.. _introduction:

############
Introduction
############

What is Gatling?
================

Gatling is a stress tool: use it to generate a population of virtual users that will create traffic on your application. Its main focus is the HTTP protocol, but it can easily be extended to support other protocols as well.

Typical goals are:

  * Anticipating problems before they actually happen.
  * Reproducing problems that happened and investing them.

Thanks to the :ref:`Assertion API <assertions>`, Gatling is also suited for functional tests, typically on Web API, thus mutualizing the functional and performance testing efforts.

Why Gatling?
============

Gatling is born from the following requirements:

  * *scenarios* that are code:

    * a `DSL <http://en.wikipedia.org/wiki/Domain-specific_language>`_ is more self explanatory than a graphical user interface.
    * code is more convenient for developers.
    * one single way of doing things: out-of-the-box features and user custom hacks use the same technology.
    * code is easier to maintain and can be injected into a version control system (VCS).

  * resource efficient, saturate the NIC before considering scaling out.
  * runs on a `JVM <http://en.wikipedia.org/wiki/Java_virtual_machine>`_, a widely-spread platform for which the administration is commonly known.


Technical Background
====================

Gatling runs on a JVM, preferably on an up-to-date JDK7.

Gatling's architecture is asynchronous as long as the underlying protocol, such as HTTP, can be implemented in a non blocking way.

This kind of architecture let us implement virtual users as messages instead of dedicated threads, making them very cheap. Thus, running thousands of concurrent virtual users is not an issue.

Gatling's core orchestration engine is based on the `Actor Model <http://en.wikipedia.org/wiki/Actor_model>`_  and is implemented on top of `Akka <http://akka.io>`_.

Gatling's HTTP engine is based on `Java NIO <http://docs.oracle.com/javase/7/docs/api/java/nio/package-summary.html>`_ and is implemented of top of `Netty <http://netty.io>`_ and `AsyncHttpClient <https://github.com/AsyncHttpClient/async-http-client>`_.
