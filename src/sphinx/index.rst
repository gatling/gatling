#######
Gatling
#######

Gatling is a highly capable load testing tool.
It is designed for ease of use, maintainability and high performance.


Out of the box, Gatling comes with an excellent support of the HTTP protocol and makes it a tool of choice for load testing any HTTP server.
As the core engine is actually protocol agnostic, it is perfectly possible to implement other protocols support.
For example, it currently also ships JMS support.

The :ref:`quickstart <quickstart>` has an overview of the most important concepts, walking you through the setup of a simple scenario for load testing an HTTP server.

Having *scenarios* that are code and being resource efficient are the two requirements that motivated us to create Gatling. Based on an expressive `DSL <http://en.wikipedia.org/wiki/Domain-specific_language>`_, the *scenarios* are self explanatory. They are easy to maintain and can be kept into a version control system.

Gatling's architecture is asynchronous as long as the underlying protocol, such as HTTP, can be implemented in a non blocking way. This kind of architecture let us implement virtual users as messages instead of dedicated threads, making them very cheap. Thus, running thousands of concurrent virtual users is not an issue.

User's guide
============

.. toctree::
   :maxdepth: 1

   quickstart
   advanced_tutorial
   general/index
   session/index
   http/index
   jms
   graphite/index
   extensions/index
   cookbook/index
   developing_gatling/index
   project/index
