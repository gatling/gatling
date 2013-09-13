#######################
Underlying Technologies
#######################

Akka Actors
===========

Gatling doesn't use the **1 thread = 1 user** paradigm.

As a matter of fact, it is too hard for the JVM to handle hundreds of threads correctly with respect to the scenario written by the tester, especially when it comes to pauses that are usually done with ``Thread.sleep()``, which is everything but precise and a waste of resources.

The model used in Gatling is the actor model which consists in asynchronous parallel computing. This is achieved by independent entities named actors that have:

* A Mailbox
* A Computation Unit

Actors communicate with each other by sending messages to other actors' mailboxes. The computation unit consumes the message and executes the action required. It carries on as long as it receives messages.

The actor model is an easy way to do parallel computing, without the hassle of managing threads, locks etc. Plus, its asynchronous nature makes it very powerful, scalable while being easy to understand.

The Actor implementation used in Gatling is `Akka <http://akka.io>`_.

Scala
=====

`Scala <http://www.scala-lang.org>`_ is a powerful language that sits on the JVM. It puts together Object and Functional paradigms.

The reason why Scala was chosen for developping Gatling was, in the first place, for it being the language of choice for working with Akka.
In the end, the Scala language was a huge help for building powerful and yet clean an simple APIs and DSLs.

Asynchronous HTTP Client and Netty
==================================

To take advantage of the asynchronous nature of the actor model, and to optimize performances, Gatling uses an asynchronous HTTP Client library which is named after its purpose: `Async HTTP Client <https://github.com/AsyncHttpClient/async-http-client>`_.
This library is an abstraction layer with a nice DSL for building and sending async requests.

We use `Netty <http://www.netty.io>`_ as the underlying HTTP technology.

Highstock and Highcharts
========================

Gatling uses the Highstock and Highcharts javascript libraries for report vizualisation.

.. note ::
	Highcharts reporting is provided in a module appart from Gatling as Highcharts and Highstock are :ref:`licensed <license>` under an OEM licence. Project is hosted `here <https://github.com/excilys/gatling-highcharts>`_.