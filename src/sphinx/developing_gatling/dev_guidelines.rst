.. _dev-guidelines:

####################
Developer Guidelines
####################

.. note::

  First read `Gatling contribution guidelines <https://github.com/excilys/gatling/blob/master/CONTRIBUTING.md>`_

Code Style
==========

The Gatling code style follows the `Scala Style Guide <http://docs.scala-lang.org/style/>`__.

As Gatling uses `Scalariform` as part of the SBT build, you don't need to worry about formatting the code :
running ``sbt compile`` will format the code automatically.

Testing
=======

All Gatling tests use `ScalaTest <http://www.scalatest.org>`__.

A few helpers are provided for testing actors or some of Gatling's APIs:

``ValidationValues``
--------------------

When tests involves Gatling's ``Validation`` API, ``ValidationValues`` provides helpers that allows to match on the ``Validation``'s result.

Example: `ELSpec <https://github.com/excilys/gatling/blob/master/gatling-core/src/test/scala/io/gatling/core/session/el/ELSpec.scala>`__

``ActorSupport``
----------------

``ActorSupport`` is meant to be used when Akka actors are to be involved in the test.


``ActorSupport`` relies on Akka's TestKit (documentation `here <http://doc.akka.io/docs/akka/2.2.4/scala/testing.html>`__) and allows you
to provide a specific Gatling configuration if needed.

Example: `PaceSpec <https://github.com/excilys/gatling/blob/master/gatling-core/src/test/scala/io/gatling/core/action/PaceSpec.scala>`__

``MockServerSupport``
---------------------

``MockServerSupport`` allows to configure and expose a Spray-can server that Gatling can make HTTP request against.

``MockServerSupport`` also exposes methods to assert that requests were indeed made to the server.

Example: `HttpIntegrationSpec <https://github.com/excilys/gatling/blob/master/gatling-http/src/test/scala/io/gatling/http/integration/HttpIntegrationSpec.scala>`__
