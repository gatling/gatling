---
title: "JMS"
description: "How to use the Java Message Service (JMS) support in Gatling to connect to a broker and perform checks against inbound messages."
lead: "DSL for JMS"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 2060000
---

JMS support was initially contributed by [Jason Koch](https://github.com/jasonk000).

## Prerequisites

Gatling JMS DSL is not imported by default.

You have to manually add the following imports:

{{< include-code "imprts" java kt scala >}}

## JMS Protocol {#protocol}

Use the `jms` object in order to create a JMS protocol.

#### `connectionFactory`

The first mandatory step is to configure the `ConnectionFactory`.

You can either configure one to be retrieved with a JNDI lookup:

{{< include-code "jndi" java kt scala >}}

or directly instantiate one with your JMS broker's Java client library, eg:

{{< include-code "prog" java kt scala >}}

#### Other Options

{{< include-code "options" java kt scala >}}

## JMS Request

Use the `jms("requestName")` method in order to create a JMS request.

### Request Type

Currently, `requestReply` and `send` (fire and forget) requests are supported.

### Destination

Define the target destination with `queue("queueName")` or alternatively with `destination(JmsDestination)`.

Optionally, you can define a reply destination with `replyQueue("responseQueue")` or `replyDestination(JmsDestination)`. Otherwise, Gatling will use a dynamic queue.

If you do so, you have the possibility of not setting the `JMSReplyTo` header with `noJmsReplyTo`.

Additionally, for the reply destination, you can define a JMS selector with `selector`

If you have the need to measure the time when a message arrive at a message queue different from the `replyDestination(JmsDestination)`,
you can additionally define a `trackerDestination(JmsDestination)`.

### Message

* `textMessage`
* `bytesMessage`
* `mapMessage`
* `objectMessage` for `java.io.Serializable` payloads

See below for a few examples:

{{< include-code "message" java kt scala >}}

### Extra Options

* `jmsType`
* `property`

{{< include-code "extra" java kt scala >}}

## JMS Check

Gatling JMS's support only current supports the following checks:
* [`bodyBytes`]({{< ref "../core/check#bodybytes" >}})
* [`bodyLength`]({{< ref "../core/check#bodylength" >}})
* [`bodyString`]({{< ref "../core/check#bodystring" >}})
* [`substring`]({{< ref "../core/check#substring" >}})
* [`jsonPath`]({{< ref "../core/check#jsonpath" >}})
* [`jmesPath`]({{< ref "../core/check#jmespath" >}})
* [`xpath`]({{< ref "../core/check#xpath" >}})

In addition, there's `simpleCheck`:

{{< include-code "simple" java kt scala >}}

## Example

Short example, assuming ActiveMQ on localhost, using a `reqReply` query, to the queue named "jmstestq":

{{< include-code "example-simulation" java kt scala >}}
