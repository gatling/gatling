#######
Gatling
#######

Gatling is a highly capable load testing tool.
It is designed for ease of use, maintainability and high performance.


Out of the box, Gatling comes with excellent support of the HTTP protocol that makes it a tool of choice for load testing any HTTP server.
As the core engine is actually protocol agnostic, it is perfectly possible to implement support for other protocols.
For example, Gatling currently also ships JMS support.

The :ref:`quickstart` has an overview of the most important concepts, walking you through the setup of a simple scenario for load testing an HTTP server.

Having *scenarios* that are defined in code and are resource efficient are the two requirements that motivated us to create Gatling. Based on an expressive `DSL <http://en.wikipedia.org/wiki/Domain-specific_language>`_, the *scenarios* are self explanatory. They are easy to maintain and can be kept in a version control system.

Gatling's architecture is asynchronous as long as the underlying protocol, such as HTTP, can be implemented in a non blocking way. This kind of architecture lets us implement virtual users as messages instead of dedicated threads, making them very resource cheap. Thus, running thousands of concurrent virtual users is not an issue.

Migrating from a Previous Version of Gatling
============================================

* If you're migrating from Gatling 2.1 to Gatling 2.2, please check the :ref:`dedicated migration guide <2.1-to-2.2>`.
* Otherwise, please follow the :ref:`previous migration guides <migration-guides>`.

User's guide
============

.. toctree::
    :maxdepth: 1

    whats_new/index
    migration_guides/index
    quickstart
    advanced_tutorial
    general/index
    session/index
    http/index
    jms
    realtime_monitoring/index
    extensions/index
    cookbook/index
    developing_gatling/index
    project/index
