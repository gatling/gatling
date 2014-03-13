###########
Session API
###########

Sessions are the actual messages that are passed along a scenario workflow.
Just like Java's HttpSession, it can be seen as a Map storage for data specific to a given virtual user.

Beware that Sessions are immutable!

Injecting data into the Session
===============================

Purpose
-------

In load testing, it's very important that the virtual users don't play the same data. Otherwise, you might be testing your caches instead of your application. Moreover, if you're running an application on a Java Virtual Machine, the Just In Time compiler (JIT) will make dramatic optimizations and your system will behave very differently from your actual one.

Though, you have to inject specific data into your virtual users/session.

Manually
--------

Session has the following methods:

* ``set(key: String, value: Any): Session``: add or replace an attribute
* ``setAll(newAttributes: (String, Any)*): Session``: bulk add or replace attributes
* ``setAll(newAttributes: Iterable[(String, Any)]): Session``: same as above but takes an Iterable instead of a varags

Note something very important and a common pitfall: those methods return a new instance of Session!

Indeed, Session is **immutable**! Why is that so? Because Sessions are messages that are dealt with in a multithreaded concurrent way, so immutability is the best way to deal with state without relying on synchronization and blocking.

bad::

	val session: Session = ???
	session.set("foo", "FOO") // wrong: the result of this set call is just discarded
	session.set("bar", "BAR")


good::

	val session: Session = ???
	session.set("foo", "FOO").set("bar", "BAR")


Check's saveAs
--------------

Gatling Checks can let one extract data from responses and save it into the Session. See Checks documentation.

Fetching data from the Session
==============================

Manually
--------

Let's say a Session instance variable named session contains a String attribute named "foo".
::

	val session: Session = ???

Then::

	val attribute: SessionAttribute = session("foo")

``session("foo")`` doesn't return the value, but a wrapper. It lets you access methods to retreive the value in several ways:

``session("foo").as[String]``:

	* returns a ``String``,
	* throws a ``NoSuchElementException`` if the "foo" attribute is undefined,
	* throws a ``ClassCastException`` if the value is not a String

``session("foo").asOption[String]``:

* returns an ``Option[String]``
* which is ``None`` if the "foo" attribute is undefined,
* throws a ``ClassCastException`` if the value is not a String

``session("foo").validate[String]``:

* returns an ``Validation[String]``
* which is a ``Failure`` if the "foo" attribute is undefined
* which is a ``Failure`` if the value is not a String


As an example, let's says we want to use the Gatling HTTP DSL and define a GET request where a query parameter was.