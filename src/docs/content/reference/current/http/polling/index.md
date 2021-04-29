---
title: "HTTP Polling"
description: "HTTP polling is an extension to the HTTP DSL"
lead: "Learn how to start and stop the polling"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
toc: true
---

HTTP polling is an extension to the HTTP DSL, whose entry point is the `polling` method.

## Common operations

If you want to deal with several pollers with per virtual users,
you have to give them a name and pass this name on each polling operation:

`pollerName(name:String)`

For example:

```scala
||< include-static "PollingSample.scala#pollerName" >||
```

Of course, this step is not required if you deal with one single poller per virtual user.

### Start polling

The first thing to do is start the polling, by specifying the request and how often it will run:

`every(period).exec(request)`

The `period` parameter is from response received to next request sent.

For example:

```scala
||< include-static "PollingSample.scala#pollerStart" >||
```

### Stop polling

When you don't need to poll a request anymore, you can stop the poller:

`poller.stop`

For example:

```scala
||< include-static "PollingSample.scala#pollerStop" >||
```

{{< alert tip >}}
Stopping a poller works in the same fashion as SSE or WebSockets `reconcile`:
When stopping a poller, the poller flow state (e.g. the session) is merged with the main flow state.
{{< /alert >}}
