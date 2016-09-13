.. _jms:

###
JMS
###

JMS support was initially contributed by `Jason Koch <https://github.com/jasonk000>`_.

Prerequisites
=============

Gatling JMS DSL is not available by default.

One has to manually add the following imports:

.. includecode:: code/Jms.scala#imports

JMS Protocol
============

.. _jms-protocol:

Use the ``jms`` object in order to create a JMS protocol.

* ``connectionFactoryName``: mandatory
* ``url``: mandatory
* ``contextFactory``: mandatory
* ``credentials``: optional, for performing JNDI lookup
* ``disableAnonymousConnect``: optional, by default, connection won't use the above credentials
* ``listenerCount``: the number of ReplyConsumers. mandatory (> 0)
* ``useNonPersistentDeliveryMode`` / ``usePersistentDeliveryMode``: optional, default to non persistent
* ``matchByMessageID`` / ``matchByCorrelationID`` / ``messageMatcher``: specify how request and response messages should be matched, default to matchByMessageID. Use matchByCorrelationID for ActiveMQ.
* ``receiveTimeout``: optional receive timeout for JMS receive method, default is 0 - infinite

JMS Request API
===============

.. _jms-request:

Use the ``jms("requestName")`` method in order to create a JMS request.

Request Type
------------

Currently, ``reqreply`` and ``send`` (fire and forget) requests are supported.

Destination
-----------

Define the target destination with ``queue("queueName")`` or alternatively with ``destination(JmsDestination)``

Optionally define reply destination with ``replyQueue("responseQueue")`` or ``replyDestination(JmsDestination)`` if not defined dynamic queue will be used.

Additionally for reply destination JMS selector can be defined with ``selector("selector")``


Message Matching
----------------

Request/Reply messages are matched using JMS pattern (request JMSMessageID should be return in response as JMSCorrelationID).

If different logic is required, it can be specified using ``messageMatcher(JmsMessageMatcher)``.

Message
-------

* ``textMessage(Expression[String])``
* ``bytesMessage(Expression[Array[Byte]])``
* ``mapMessage(Expression[Map[String, Any]])``
* ``objectMessage(Expression[java.io.Serializable])``

Properties
----------

One can send additional properties with ``property(Expression[String], Expression[Any])``.

JMS Type
----------

Jms type can be specified with ``jmsType(Expression[String])``.

JMS Check API
=============

.. _jms-api:

JMS checks are very basic for now.

There is ``simpleCheck`` that accepts just ``javax.jms.Message => Boolean`` functions.

There is also ``xpath`` check for ``javax.jms.TextMessage`` that carries XML content.

Additionally you can define your custom check that implements ``Check[javax.jms.Message]``

Example
=======

Short example, assuming FFMQ on localhost, using a reqreply query, to the queue named "jmstestq":

.. includecode:: code/Jms.scala#example-simulation

