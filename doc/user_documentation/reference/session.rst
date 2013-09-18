.. _session:

#######
Session
#######

The Session is a storage attached to each "virtual user".

Using ``saveAs("key")`` actually stores the data into the Session, under the given key.

Using a ``${...}`` EL in a String retrieves the data stored in the Session under the given key.

.. _session-el:

Expression Language
===================

Gatling supports very basic Expression Language that can be used for making String parameters dynamic, such as URLs, HTTP parameters, HTTP basic auth credentials, etc.

``"${myKey}"`` returns the Session attribute stored as ``myKey``

``"${myKey(i)}"`` where ``i`` is an Integer returns the value in the multivalued Session attribute stored as ``myKey`` with the rank ``i`` 

``"${myKey.size()}"`` returns the size of the multivalued Session attribute stored as ``myKey``

``"${myKey(myRank)}"`` where ``myRank`` is a String returns the value in the multivalued Session attribute stored as ``myKey`` with the rank that is the value of the Session attribute ``myRank``

For example, if a given Session contains ``("myValue" -> "foo")``, ``("myList" -> List("bar", baz))`` and ``("myInteger" -> 1)``:

* ``"AAA${myValue}BBB"`` will be resolved as ``"AAAfooBBB"``
* ``"AAA${myList(0)}BBB"`` will be resolved as ``"AAAbarBBB"``
* ``"AAA${myList(myInteger)}BBB"`` will be resolved as ``"AAAbazBBB"``
* ``"AAA${myList.size()}"`` will be resolved as ``2``

Immutability
============

The most important characteristic of Sessions is that they are **immutable**.

As Gatling engine is completely asynchronous, immutability garanties that state desynchronization can't happen when moving from one core to another.

As a consequence, editing the Session actually generates a new Session instance and leaves the original instance untouched.

Session members
===============

* ``getAttribute(key: String): Any = getTypedAttribute[Any](key)``
* ``getTypedAttribute[X](key: String)``
* ``getAttributeAsOption[T](key: String): Option[T]``
* ``setAttributes(attributes: Map[String, Any])``
* ``setAttribute(attributeKey: String, attributeValue: Any)``
* ``removeAttribute(attributeKey: String)``
* ``isAttributeDefined(attributeKey: String)``
* ``getCounterValue(counterName: String)``
* ``getTimerValue(timerName: String)``

Building Session functions
==========================

Sometimes, one might want to manipulate the Session is a way that is not built in Gatling, like printing for debugging or adding custom attributes.

In this case, Gatling provides a hack for editing the Session programmatically.

.. note:: As Session is immutable, so when writing a Session function, one has to ensure that it returns the new Session.

::

	.exec(session => {
	  // print the Session for debugging, don't do that on real Simulations
	  println(session)
	  session
	})
	.exec(session =>
	  // session.setAttribute returns the new Session, and Scala automatically returns the last assignment
	  // braces are omitted as there's only one instruction
	  session.setAttribute("foo", "bar")
	)