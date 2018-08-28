.. _jms:

###
JMS
###

JMS support was initially contributed by `Jason Koch <https://github.com/jasonk000>`_.

Prerequisites
=============

Gatling JMS DSL is not available by default.

One has to manually add the following imports:

.. includecode:: code/JmsSample.scala#imports

JMS Protocol
============

.. _jms-protocol:

Use the ``jms`` object in order to create a JMS protocol.

* ``connectionFactory``: mandatory, an instance of `ConnectionFactory`. Use `jmsJndiConnectionFactory`_ to obtain one via JNDI lookup or create it by yourself.
* ``credentials``: optional, to create a JMS connection
* ``useNonPersistentDeliveryMode`` / ``usePersistentDeliveryMode``: optional, default to non persistent
* ``matchByMessageID`` / ``matchByCorrelationID`` / ``messageMatcher``: specify how request and response messages should be matched, default to matchByMessageID. Use matchByCorrelationID for ActiveMQ.
* ``replyTimeout``: optional reply timeout, in milliseconds, default is none
* ``listenerThreadCount``: optional listener thread count, some JMS implementation (like IBM MQ) need more than on MessageListener to achieve full readout performance

JMS JNDI Connection Factory
===========================

Use `jmsJndiConnectionFactory` object to obtain an instance of JMS `ConnectionFactory` via JNDI lookup.

.. _jmsJndiConnectionFactory:

* ``connectionFactoryName``: mandatory
* ``url``: mandatory
* ``contextFactory``: mandatory
* ``credentials``: optional, for performing JNDI lookup
* ``property``: optional, custom JNDI property

JMS Request API
===============

.. _jms-request:

Use the ``jms("requestName")`` method in order to create a JMS request.

Request Type
------------

Currently, ``requestReply`` and ``send`` (fire and forget) requests are supported.

Destination
-----------

Define the target destination with ``queue("queueName")`` or alternatively with ``destination(JmsDestination)``

Optionally define reply destination with ``replyQueue("responseQueue")`` or ``replyDestination(JmsDestination)``, otherwise a dynamic queue will be used.
If you do so, you have to possibility of not setting the `JMSReplyTo` header with ``noJMSReplyTo``.

Additionally for reply destination JMS selector can be defined with ``selector("selector")``

If you have the need, to measure the time when a message arrive at a different message queue then the ``replyDestination(JmsDestination)``
you can additional define a ``trackerDestination(JmsDestination)``.


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
--------

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

.. includecode:: code/JmsSample.scala#example-simulation
