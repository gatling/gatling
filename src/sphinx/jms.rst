.. _jms:

###
JMS
###

JMS support was initially contributed by Jason Koch.

Prerequisites
=============

Gatling JMS DSL is not available by default.

One has to manually add the following imports::

  import io.gatling.jms.Predef._
  import javax.jms._

JMS Protocol
============

.. _jms-protocol:

Use the ``jms`` object in order to create a JMS protocol.

* ``connectionFactoryName``: mandatory
* ``url``: mandatory
* ``contextFactory``: mandatory
* ``credentials``: optional
* ``listenerCount``: the number of ReplyConsumers. mandatory (> 0)
* ``useNonPersistentDeliveryMode``/``usePersistentDeliveryMode``: optional, default to non persistent

JMS Request API
===============

.. _jms-request:

Use the ``jms("requestName")`` method in order to create a JMS request.

Request Type
------------

Currently, only ``reqreply`` request type is supported.

Queue
-----

Define the target queue with ``queue("queueName")``.

Message
-------

* ``textMessage(Expression[String])``
* ``bytesMessage(Expression[Array[Byte]])``
* ``mapMessage(Expression[Map[String, Any]])``
* ``objectMessage(Expression[java.io.Serializable])``

Properties
----------

One can send additional properties with ``property(Expression[String], Expression[String])``.

JMS Check API
=============

.. _jms-api:

JMS checks are very basic for now.

There's just ``javax.jms.Message => Boolean`` functions

Example
=======

Short example, assuming FFMQ on localhost, using a reqreply query, to the queue named "jmstestq"::

  import net.timewalker.ffmq3.FFMQConstants
  import io.gatling.core.Predef._
  import io.gatling.jms.Predef._
  import javax.jms._
  import scala.concurrent.duration._

  class TestJmsDsl extends Simulation {

    val jmsConfig = JmsProtocolBuilder.default
      .connectionFactoryName(FFMQConstants.JNDI_CONNECTION_FACTORY_NAME)
      .url("tcp://localhost:10002")
      .credentials("user", "secret")
      .contextFactory(FFMQConstants.JNDI_CONTEXT_FACTORY)
      .listenerCount(1)
      .usePersistentDeliveryMode

    val scn = scenario("JMS DSL test").repeat(1) {
      exec(jms("req reply testing").reqreply
      .queue("jmstestq")
      .textMessage("hello from gatling jms dsl")
      .property("test_header", "test_value")
      .check(checkBodyTextCorrect)
      )
    }

    setUp(scn.inject(rampUsersPerSec(10) to (1000) during (2 minutes)))
      .protocols(jmsConfig)

    def checkBodyTextCorrect(m: Message) = {
      // this assumes that the service just does an "uppercase" transform on the text
      m match {
      case tm: TextMessage => tm.getText.toString == "HELLO FROM GATLING JMS DSL"
      case _ => false
      }
    }
  }
