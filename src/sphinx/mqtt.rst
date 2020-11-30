.. _mqtt:

####
MQTT
####

MQTT support is only available in `FrontLine <https://gatling.io/gatling-frontline/>`__.

Jar published in Gatling OSS only contains noop stubs.

It only supports MQTT 3.1 and 3.1.1. More recent versions are not currently supported.

Prerequisites
=============

Gatling FrontLine MQTT DSL is not imported by default.

One has to manually add the following imports::

  import io.gatling.mqtt.Predef._

MQTT Protocol
=============

.. _mqtt-protocol:

Use the ``mqtt`` object in order to create a MQTT protocol.

* ``mqttVersion_3_1``: enable protocol version 3.1 (default: false)
* ``mqttVersion_3_1_1``: enable protocol version 3.1.1 (default: true)
* ``broker("hostname", port)``: broker address (default: localhost:1883)
* ``useTls(boolean)``: if TLS should be enabled (default: false)
* ``clientId("id")``: clientIdentifier sent in the connect payload (of not set, Gatling will generate a random one)
* ``cleanSession(boolean)``: if session should be cleaned during connect (default: true)
* ``credentials("${userName}", "${password}")``: optional credentials for connecting
* ``keepAlive(30)``: connections keep alive timeout
* ``qosAtMostOnce``: use at-most-once QoS (default: true)
* ``qosAtLeastOnce``: use at-least-once QoS (default: false)
* ``qosExactlyOnce``: use exactly-once QoS (default: false)
* ``retain(true)``: enable retain (default: false)
* ``lastWill(LastWill("${willTopic}", StringBody("${willMessage}")).qosAtLeastOnce.retain(true))``: send last will, possibly with specific QoS and retain
* ``reconnectAttemptsMax(1)``: max number of reconnects after connection crash (default: 3)
* ``reconnectDelay(1L)``: reconnect delay after connection crash in millis (default: 100)
* ``reconnectBackoffMultiplier(1.5F)``: reconnect delay exponential backoff (default: 1.5)
* ``resendDelay(1000)``: resend delay after send failure in millis (default: 5000)
* ``resendBackoffMultiplier(2.0F)``: resend delay exponential backoff (default: 1.0)
* ``timeoutCheckInterval(1 second)``: interval for timeout checker (default: 1 second)
* ``correlateBy(check)``: check for pairing messages sent and messages received

MQTT Request API
================

.. _mqtt-request:

Use the ``mqtt("requestName")`` method in order to create a MQTT request.


Connect
-------

Use the ``connect`` method to connect to the MQTT broker::

  mqtt("Connecting").connect

Subscribe
---------

Use the ``subscribe`` method to subscribe to an MQTT topic::

  mqtt("Subscribing")
    .subscribe("${myTopic}")
    .qosAtMostOnce // override default QoS

Publish
-------

Use the ``publish`` method to publish a message. You can use the same ``Body`` API as for HTTP request bodies::

  mqtt("Publishing")
    .publish("${myTopic}")
    .message(StringBody("${myTextPayload}"))


.. mqtt-check:

MQTT Checks
===========

You can define blocking checks with ``await`` and non-blocking checks with ``expect``.
Those can be set right after subscribing, or after publishing::

  // subscribe and expect to receive a message within 100ms, without blocking flow
  mqtt("Subscribing").subscribe("${myTopic2}")
    .expect(100 milliseconds)
  // publish and wait (block) until it receives a message withing 100ms
  mqtt("Publishing").publish("${myTopic}").message(StringBody("${myPayload}"))
    .wait(100 milliseconds)

You can optionally define in which topic the expected message will be received::

  .wait(100 milliseconds, "repub/${myTopic}")

You can optionally define check criteria to be applied on the matching received message::

  mqtt("Publishing")
    .publish("${myTopic}").message(StringBody("${myPayload}"))
    .wait(100 milliseconds).check(jsonPath("$.error").notExists)

You can use ``waitForMessages`` and block for all pending non-blocking checks::

  exec(waitForMessages.timeout(100 milliseconds))

.. mqtt-conf:

MQTT configuration
==================

MQTT support honors the ssl and netty configurations from ``gatling.conf``.

Example
=======

::

  import scala.concurrent.duration._
  import io.gatling.core.Predef._
  import io.gatling.mqtt.Predef._

  class MqttSample {

    private val mqttConf = mqtt
      .broker("localhost", 1883)
      .correlateBy(jsonPath("$.correlationId"))

    private val scn = scenario("MQTT Test")
      .feed(csv("topics-and-payloads.csv"))
      .exec(mqtt("Connecting").connect)
      .exec(mqtt("Subscribing").subscribe("${myTopic}"))
      .exec(mqtt("Publishing").publish("${myTopic}").message(StringBody("${myTextPayload}"))
        .expect(100 milliseconds).check(jsonPath("$.error").notExists))

    setUp(scn.inject(rampUsersPerSec(10) to 1000 during (2 minutes)))
      .protocols(mqttConf)
  }
