.. _dev-guidelines:

####################
Developer Guidelines
####################

.. note::

  First read `Gatling contribution guidelines <https://github.com/gatling/gatling/blob/master/CONTRIBUTING.md>`_

Code Style
==========

The Gatling code style follows the `Scala Style Guide <http://docs.scala-lang.org/style/>`__.

As Gatling uses `Scalariform` as part of the SBT build, you don't need to worry about formatting the code :
running ``sbt compile`` will format the code automatically.

Testing
=======

All Gatling tests use `ScalaTest <http://www.scalatest.org>`__.

A few helpers are provided for testing actors or some of Gatling's APIs:

``BaseSpec``
------------

BaseSpec must be used on every test, as it sets out the structure and commons helpers for Gatling tests


``ValidationValues``
--------------------

When tests involves Gatling's ``Validation`` API, ``ValidationValues`` provides helpers that allows to match on the ``Validation``'s result.

Example: `ELSpec <https://github.com/gatling/gatling/blob/master/gatling-core/src/test/scala/io/gatling/core/session/el/ElSpec.scala>`__

``AkkaSpec``
------------

``ActorSupport`` is meant to be used when Akka actors are to be involved in the test.


``ActorSupport`` relies on Akka's TestKit (documentation `here <http://doc.akka.io/docs/akka/current/scala/testing.html>`__).

Example: `PaceSpec <https://github.com/gatling/gatling/blob/master/gatling-core/src/test/scala/io/gatling/core/action/PaceSpec.scala>`__

``HttpSpec``
------------

``HttpSpec`` allows to configure and expose a simple Netty server that Gatling can make HTTP requests against.

``HttpSpec`` also exposes methods to assert that requests were indeed made to the server.

Example: `HttpIntegrationSpec <https://github.com/gatling/gatling/blob/master/gatling-http/src/test/scala/io/gatling/http/integration/HttpIntegrationSpec.scala>`__
