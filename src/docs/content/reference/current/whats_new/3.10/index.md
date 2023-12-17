---
title: "What's New in 3.10"
description: "Gatling 3.10 release notes"
lead: ""
date: 2023-12-14T23:00:00+02:00
lastmod: 2023-12-14T23:00:00+02:00
weight: 2020097
---

For more detailed release notes, including patch releases, please check the [milestones on GitHub](https://github.com/gatling/gatling/milestones?state=closed).

{{< alert warning >}}
Beware that Gatling 3.10 introduces a few breaking changes. Make sure to check the [Upgrade guide]({{< ref "../../upgrading/3.9-to-3.10.md" >}})
{{</ alert >}}

## Contributors

A huge thank you to our great community members who helped with this release:
* [Amerousful](https://github.com/Amerousful)
* [andrzejwld](https://github.com/andrzejwld)
* [drrc1709](https://github.com/drrc1709)
* [gebhardt](https://github.com/gebhardt)
* [gemiusz](https://github.com/gemiusz)
* [ismail-s](https://github.com/ismail-s)
* [jonathanmash](https://github.com/jonathanmash)
* [kudoueiji](https://github.com/kudoueiji)
* [micheljung](https://github.com/micheljung)
* [mrdingma](https://github.com/mrdingma)
* [phiSgr](https://github.com/phiSgr)
* [presidentio](https://github.com/presidentio)
* [ViliusS](https://github.com/ViliusS)

## Core

* [#4486](https://github.com/gatling/gatling/issues/4486), [#4489](https://github.com/gatling/gatling/issues/4489) and [#4492](https://github.com/gatling/gatling/issues/4492) greatly improve the Java and Scala SDKs' usage.

Now, instead of always chaining `ActionBuilder`s and `ChainBuilder`s with the `exec` method, you can now pass a whole sequence at once.

This new syntax greatly reduces the verbosity and the amount of `exec` calls.
It's also more code-formatting friendly and reduces the amount of indentations.

Typically, instead of writing:

{{< include-code "oldExec" java >}}

you can now write:

{{< include-code "newExec" java >}}

Note: the old syntax is of course still valid.

## HTTP

* [#4422](https://github.com/gatling/gatling/issues/4422): Support all OAuth1 signature modes, not just an `Authorization` header

## HTML Reports

* [#4479](https://github.com/gatling/gatling/pull/4479): New Dark theme
