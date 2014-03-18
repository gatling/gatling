.. _session_api:

###########
Session API
###########

``Session``\ s are the actual messages that are passed along a scenario workflow.

A Session can be seen as a Map storage for data specific to a given virtual user.

Injecting data into the Session
===============================

Purpose
-------

In load testing, it's very important that the virtual users don't play the same data. Otherwise, you might be testing your caches instead of your application.

Moreover, if you're running an application on a Java Virtual Machine, the Just In Time compiler (JIT) will make dramatic optimizations and your system will behave very differently from your actual one.

Though, you have to inject specific data into your virtual users/session.

Manually
--------

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

Feeders
-------

See `Feeders documentation <feeder.html>`_.

Check's saveAs
--------------

Gatling Checks can let one extract data from responses and save it into the Session. See `Checks documentation <../http/http_check.html>`_.

Fetching data from the Session
==============================

Manually
--------

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

Using Gatling EL
----------------

See `Gatling EL documentation <expression_el.html>`_.
