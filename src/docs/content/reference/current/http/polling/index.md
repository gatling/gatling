---
title: "HTTP Polling"
description: "How to perform HTTP polling in your tests."
lead: "Learn how to start and stop the polling"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
toc: true
weight: 2050700
---

HTTP polling is an extension to the HTTP DSL, whose entry point is the `poll` method.

## Common operations

If you want to deal with several pollers with per virtual users,
you have to give them a name and pass this name on each polling operation:

`pollerName(name:String)`

For example:

{{< include-code "pollerName" java kt scala >}}

Of course, this step is not required if you deal with one single poller per virtual user.

### Start polling

The first thing to do is start the polling, by specifying the request and how often it will run:

`every(period).exec(request)`

The `period` parameter is from response received to next request sent.

For example:

{{< include-code "pollerStart" java kt scala >}}

{{< alert warning >}}
Currently, polling doesn't support checks. If you define some checks on your polled requests, they won't do anything.
{{</ alert >}}

### Stop polling

When you don't need to poll a request anymore, you can stop the poller:

`poll.stop`

For example:

{{< include-code "pollerStop" java kt scala >}}

{{< alert tip >}}
When stopping a poller, the poller flow state (e.g. the session) is merged with the main flow state.
{{< /alert >}}
