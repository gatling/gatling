---
title: "MQTT"
description: "MQTT protocol DSL"
lead: "DSL for MQTT"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 006000
---

MQTT support is only available in [Gatling Enterprise](https://gatling.io/enterprise/).

Jar published in Gatling OSS only contains noop stubs.

It only supports MQTT 3.1 and 3.1.1. More recent versions are not currently supported.

## Prerequisites

Gatling Enterprise MQTT DSL is not imported by default.

You have to manually add the following imports:

{{< include-code "imprts" java kt scala >}}

## MQTT Protocol

Use the `mqtt` object in order to create a MQTT protocol.

{{< include-code "protocol" java kt scala >}}

## Request

Use the `mqtt("requestName")` method in order to create a MQTT request.

### `connect`

Your virtual users first have to establish a connection.

{{< include-code "connect" java kt scala >}}

### `subscribe`

Use the `subscribe` method to subscribe to an MQTT topic:

{{< include-code "subscribe" java kt scala >}}

### `publish`

Use the `publish` method to publish a message. You can use the same `Body` API as for HTTP request bodies:

{{< include-code "publish" java kt scala >}}

## MQTT Checks

You can define blocking checks with `await` and non-blocking checks with `expect`.
Those can be set right after subscribing, or after publishing:

{{< include-code "check" java kt scala >}}

You can optionally define in which topic the expected message will be received:

You can optionally define check criteria to be applied on the matching received message:

You can use `waitForMessages` and block for all pending non-blocking checks:

{{< include-code "waitForMessages" java kt scala >}}

## MQTT configuration

MQTT support honors the ssl and netty configurations from `gatling.conf`.

## Example

{{< include-code "sample" java kt scala >}}
