.. _session:

###########
Session API
###########

.. _session-concept:

Concept
=======

Going Stateful
--------------

In most load testing use cases, it's important that the virtual users don't play the same data.
Otherwise, you might end up not testing your application but your caches.

Moreover, if you're running an application on a Java Virtual Machine, the Just In Time compiler (JIT) will make dramatic optimizations and your system will behave very differently from your actual one.

Though, **you have to make your scenario steps dynamics, based on virtual user specific data**.

Session
-------

Session is a virtual user's state.

Basically, it's a ``Map[String, Any]``: a map with key Strings.
In Gatling, entries in this map are called **Session attributes**.

.. note::
    Remember that a Gatling scenario is a workflow where every step is backed by an Akka Actor?

    ``Session``\ s are the actual messages that are passed along a scenario workflow.

Injecting Data
--------------

The first step is to inject state into the virtual users.

There's 3 ways of doing that:

    * using :ref:`Feeders <feeder>`
    * extracting data from responses and saving them, e.g. with :ref:`HTTP Check's saveAs <http-check-saveas>`
    * manually with the Session API

Fetching Data
-------------

Once you have injected data into your virtual users, you'll naturally want retrieve and use it.

There's 2 ways of doing that:

    * using Gatling's :ref:`Expression Language <el>`
    * manually with the Session API

.. _session-api:

Session API
===========

Setting Attributes
------------------

Session has the following methods:

* ``set(key: String, value: Any): Session``: add or replace an attribute
* ``setAll(newAttributes: (String, Any)*): Session``: bulk add or replace attributes
* ``setAll(newAttributes: Iterable[(String, Any)]): Session``: same as above but takes an Iterable instead of a varags

.. warning::
    ``Session`` instances are immutable!

    Why is that so? Because Sessions are messages that are dealt with in a multi-threaded concurrent way,
    so immutability is the best way to deal with state without relying on synchronization and blocking.

    A very common pitfall is to forget that ``set`` and ``setAll`` actually return new instances.

::

    val session: Session = ???

    // wrong usage
    session.set("foo", "FOO") // wrong: the result of this set call is just discarded
    session.set("bar", "BAR")

    // proper usage
    session.set("foo", "FOO").set("bar", "BAR")

Getting Attributes
------------------

Let's say a Session instance variable named session contains a String attribute named "foo".
::

	val session: Session = ???

Then::

	val attribute: SessionAttribute = session("foo")


.. warning::
    ``session("foo")`` doesn't return the value, but a wrapper.

You can then access methods to retrieve the actual value in several ways:

``session("foo").as[String]``:

	* returns a ``String``,
	* throws a ``NoSuchElementException`` if the "foo" attribute is undefined,
	* throws a ``ClassCastException`` if the value is not a String

``session("foo").asOption[String]``:

    * returns an ``Option[String]``
    * which is ``None`` if the "foo" attribute is undefined,
    * which is ``Some(value)`` otherwise and *value* is indeed a String
    * throws a ``ClassCastException`` otherwise

``session("foo").validate[String]``:

    * returns an ``Validation[String]``
    * which is ``Failure(errorMessage)`` if the *"foo"* attribute is undefined
    * which is ``Failure(errorMessage)`` if the value is not a String
    * which is ``Success(value)`` otherwise

.. note::

    Using ``as`` will probably easier for most users.
    It will work fine, but the downside is that they might generate lots of expensive exceptions once things starts going wrong under load.

    We advise considering ``validate`` once accustomed to functional logic as it deals with unexpected results in a more efficient manner.
