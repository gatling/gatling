##################
Livin' on the Edge
##################

Gatling 2 is currently under development. We've been doing massive refactoring on the inner APIs, sometimes on the outer ones. We've reached a point where we consider the APIs stable enough so that we can release milestones and start building new features on top of it.

If you're building your own protocol implementation, or did massive Scala engineering, we encourage you go with this milestone instead of producing more code against old/removed APIs.

####
Core
####

Scala 2.10
==========

Gatling 2 uses Scala 2.10. For those using an IDE, beware of using a compatible one.

Note that ``akka.util.duration`` has been migrated in the ``scala.concurrent.duration`` package of the Scala library, so you'll have to replace the import in your Simulations.

Also note that Gatling 2 is built with JDK7 and targets JDK6.

Package and GroupId
===================

Package and GroupId are now ``io.gatling``. Update your build scripts and change the imports in your Simulations.

Validation
==========

``Validation`` is a new thing in Gatling 2.

It's a simplified version of Scalaz Validation. Basically, it's a trait that has 2 implementations: ``Success`` and ``Failure`` that you can compose with ``map``, ``flatMap`` and ``filter``. Success contains the expected result and Failure a String message.

Gatling 2 is massively built on top of it, so that errors can be gracefully handled, such as when build requests, resolving EL or passing custom functions.

Expression
==========

``Expression[T]`` is an alias for ``Session => Validation[T]``. It's the compiled result of an EL expression and the expected parameter type for most DSL methods.

Session
=======

.. warning:: API changed between 2.0.0-M2 and 2.0.0-M3!

The Session has been under major refactoring. You now have to use:

* ``session(attributeName).as[T]`` for getting the casted value of an attribute. This will cause a ``NoSuchElementException`` is the attribute is missing and a ``ClassCastException`` if the type is wrong.
* ``session(attributeName).asOption[T]`` for getting an Option[T]. This will cause a ``ClassCastException`` if the type is wrong.
* ``session(attributeName).validate[T]`` for getting a Validation[T]. This will never throw an exception, but might return a ``Failure``.
* ``session.set(attributeName, attributeValue)`` for setting an attribute. Note that as Session is immutable, this method returns the new Session.


DSL
===

New Inject API
--------------

The old ``users``, ``ramp`` and ``delay`` have be removed. You now have a new fluent API that let you profile injection the way you want.

Here's an example::

	setUp(scn.inject(nothingFor(4 seconds),
	                 atOnce(10 users),
	                 ramp(10 users) over (5 seconds),
	                 constantRate(20 usersPerSec) during (15 seconds),
	                 rampRate(10 usersPerSec) to(20 usersPerSec) during(10 minutes),
	                 split(1000 users).into(ramp(10 users) over (10 seconds))
	                                  .separatedBy(10 seconds),
	                 split(1000 users).into(ramp(10 users) over (10 seconds))
	                                  .separatedBy(atOnce(30 users)))
	                 .protocols(httpConf)

New protocol set up
-------------------

* HTTP configuration bootstrapper is no longer ``httpConfig`` but plain ``http`` (since M3)
* Protocols are no longer set up per scenario but per simulation with the ``protocols`` method (since M3). Chain this method call with the one to the ``setUp`` method, see example above.

New assertions set up
---------------------

``assertThat`` has been removed, assertions are now chained with setUp::

	setUp(scn.inject(atOnce(1 user)))
	  .protocols(httpProtocol)
	  .assertions(global.responseTime.max.lessThan(1000))

Misc
----

* ``transform`` is now Option => Option, so one have full control of what's extracted.
* ``foreach``: the first parameter is now an expression, not the attribute name. So if you've been writing ``foreach("foo", "bar")``, you now have to write ``foreach("${foo}", "bar")``
* new ``.random`` EL function that randomly gets an element from a Sequence attribute
* Loops now have an additional parameter named ``exitASAP``, defaulting to true, that makes the loop condition be evaluated on every element inside the loop.

####
HTTP
####

Connections
===========

In Gatling 1, connections are shared amongst users. This behavior does not match real browsers, and doesn't support SSL session tracking.

In Gatling 2, the default behavior is that every user has his own connection pool. This can be tuned with the ``shareConnections`` configuration param.

Request Bodies
==============

.. warning:: API changed between 2.0.0-M2 and 2.0.0-M3!

* ``body(body)`` where body can be:

  * ``ELFileBody(path)`` where path is the location of a EL template file, can be a String or an Expression[String]
  * ``StringBody(string)`` where string can be a String or an Expression[String]
  * ``RawFileBody(path)`` where path is the location of a file, can be String or an Expression[String]
  * ``ByteArrayBody(bytes)`` where bytes can be an Array[Byte] or an Expression[Array[Byte]]
  * ``InputStreamBody(stream)`` where stream can be an InputStream or an Expression[InputStream]

* ``bodyPart(bodyPart)``, where bodyPart can be:

  * ``RawFileBodyPart(name, filePath, contentType)``
  * ``ELFileBodyPart(name, filePath)``
  * ``StringBodyPart(name, string, charset, contentType, transferEncoding, contentId)``
  * ``ByteArrayBodyPart(name, bytes, charset, contentType, transferEncoding, contentId)``
  * ``FileBodyPart(name, file, charset, contentType, transferEncoding, contentId)``

Note that one can take full advantage of Scala 2.10 macros for writing template directly in Scala compiled code instead of relying on a template engine. See `Scala 2.10 string interpolation <(http://docs.scala-lang.org/overviews/core/string-interpolation.html>`_ and `Fastring <https://github.com/Atry/fastring>`_.

For example::

	object Templates {
	  val template: Expression[String] = (session: Session) =>
	  for {
	    foo = session("foo").validate[String]
	    bar = session("bar").validate[String]
	  } yield s"""{
	    foo: $foo,
	    bar: $bar
	  }"""
	}

.. note:: For simple use cases, prefer EL based files, for more complex ones where programming capability is required, prefer standard String interpolation.

Processors
==========

* ``processRequestBody`` takes a ``RequestBody => RequestBody`` function and let one process the request body before it's being sent to the wire. Gatling ships 2 built-ins: ``gzipRequestBody`` and ``streamRequestBody``.
* ``processResponse`` take a ``Response => Response`` function and let one process the response before it's being sent to the checks pipeline.

Misc
====

* ``multiValuedParam`` now takes an EL expression.
* Shortcuts that didn't specify the value have been removed.
* ``exec(addCookies(url: Expression[String], cookie: Cookie, cookies: Cookie*))`` lets one add cookies to the cookie store
* use ``feed(feeder, number)`` for popping multiple lines all at once. Attribute names will be suffixed with the index.
* ``RequestStatus`` has been renamed into ``Status`` and ``ExtendedResponse`` into ``Response`` (since M3)
* info extractors have been merged into a single ``extraInfoExtractor(Status, Session, Request, Response) => List[Any]]``. Built-in ``dumpSessionOnFailure`` dumps the whole Session on request failure, use for debugging.
* ``basicAuth`` can be configured also at protocol level

########
Graphite
########

Graphite DataWriter now supports both TCP (default) and UDP.
You can configure which protocol to use to send metrics to Graphite with the `protocol` parameter in gatling.conf (`tcp` or `udp`).

####
Logs
####

``simulation.log`` file has been redesigned. Beware if you've been building your own DataFileReader.

########
Recorder
########

You can now import an HAR (Http Archive) into the Recorder and convert it to a Gatling simulation.
HAR files can be obtained using the Chrome Developer Tools or with Firebug and the NetExport Firebug extension.
To import an HAR file, select the "HAR converter" mode in the top right dropdown in the Recorder.