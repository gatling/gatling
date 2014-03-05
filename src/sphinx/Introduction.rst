************
Introduction
************

Motivation
----------

Gatling was born from the observation that the commonly used stress test
tools don't have the required performance to stress nowadays
applications. Moreover, any Java developer who used these tools was
confronted with one of these situations:

-  We can fire the tests only three times, because the license for
   LoadRunner is way too expensive...
-  Oh my ! The structure of the pages has changed, we have to edit the
   whole script via the GUI, let's start clicking...
-  Sigh! The customer chose to use OpenSTA, we do not have any choice
   but to use Windows VMs...
-  JMeter cannot take that much load (strange, isn't it ?), we have to
   build a cluster of JMeters and monitor the application AND the
   injection nodes to be sure that the failure doesn't come from
   JMeter...
-  The Grinder failed because of a memory leak, it's too hard to debug
   when you're not a Python developer...

So, why use Gatling?
--------------------

Gatling is efficient
~~~~~~~~~~~~~~~~~~~~

Gatling uses actors and async IO, so it's far more efficient that
one-thread-per-user based solutions. It gives more accurate results with
far less memory and CPU usage. What's the point in having to run the
stress tool with more horse power than the system under test?!

Gatling scripts are elegant and concise
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Simulation scripts should be written in a concise and user-friendly
manner so that they can be handled by a version control system. Why is
that? Because performing performance tests should be part of the release
process, so a version of a scenario is to be bound to a version of the
application sources, and one should be able to perform diffs between
versions. This can't be achieved if the script are huge dumps of XML
that can only be read from a specific GUI. Gatling provides a `concise,
elegant and easy to learn syntax <Structure-Elements>`__:

.. code:: scala

    scenario("Standard User")
      .exec( http("Access Github") get("http://github.com") )
      .pause(2, 3)
      .exec( http("Search for 'gatling'") get("http://github.com/search") queryParam("q","gatling") )
      .pause(2)

Gatling scripts are code
~~~~~~~~~~~~~~~~~~~~~~~~

The standard DSL should be sufficient for most use cases. However, this
DSL is actually written in Scala, on top of an open functional API. As a
consequence, it can be easily
`extended <Advanced-Usage#wiki-scala-functions>`__ with very few basic
knowledge: no need to be a Scala hacker.

Gatling charts are meaningful
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We try our best to provide charts with meaningful information and will
continue to do so with regards to user feedback. These charts use
javascripts libraries to give a rich user experience.

Gatling is free
~~~~~~~~~~~~~~~

Even if Gatling is not fully open source (due to dependencies
`licences <License>`__), it is completely free of charge!
