---
title: "SSE (Server Sent Event)"
description: "SSE support is an extension to the HTTP DSL"
lead: "Learn the possible SSE operations with Gatling: connect, close"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

SSE support is an extension to the HTTP DSL, whose entry point is the `sse(requestName: Expression[String])` method.

## Common operations

If you want to deal with several SSE streams per virtual users, you have to give them a name and pass this name on each SSE operation:

`sseName(name: String)`

For example:

```scala
||< include-static "SseSample.scala#sseName" >||
```

Of course, this step is not required if you deal with one single SSE stream per virtual user.

### Connect

The first thing is to get a server sent event:

`connect(url: Expression[String])`

For example:

```scala
||< include-static "SseSample.scala#sseConnect" >||
```

{{< alert tip >}}
Gatling automatically sets `Accept` header to `text/event-stream` and `Cache-Control` to `no-cache`.
{{< /alert >}}

### Close

When you're done with a SSE stream, you can close it:

`close`

For example:

```scala
||< include-static "SseSample.scala#sseClose" >||
```

## Server Messages: Checks

You deal with incoming messages with checks.

Beware to not miss messages that would be received prior to setting the check.

Gatling currently only supports blocking checks that will waiting until receiving expected message or timing out.

### Set a Check

You can set a check right after connecting:

```scala
||< include-static "SsSample.scala#check-from-connect" >||
```

Or you can set a check from main flow:

```scala
||< include-static "SseSample.scala#check-from-flow" >||
```

You can set multiple checks sequentially. Each one will expect one single frame.

You can configure multiple checks in a single sequence:

```scala
||< include-static "SseSample.scala#check-single-sequence" >||
```

You can also configure multiple check sequences with different timeouts:

```scala
||< include-static "SseSample.scala#check-check-multiple-sequence" >||
```

### Create a check

You can create checks for server events with `checkMessage`.
You can use almost all the same check criteria as for HTTP requests.

```scala
||< include-static "SseSample.scala#create-single-check" >||
```

You can have multiple criteria for a given message:

```scala
||< include-static "SseSample.scala#create-multiple-checks" >||
```

### Matching messages

You can define `matching` criteria to filter messages you want to check.
Matching criterion is a standard check, except it doesn't take `saveAs`.
Non matching messages will be ignored.

```scala
||< include-static "SseSample.scala#matching" >||
```

## Configuration

Server sent event support uses the same parameter as the HttpProtocol:

`baseUrl(url: String)`: serves as root that will be prepended to all relative server sent event urls

`baseUrls(urls: String*)`: serves as round-robin roots that will be prepended to all relative server sent event urls

## Debugging

In your logback configuration, lower logging level to `DEBUG` on logger `io.gatling.http.action.sse.fsm`:

```xml
<logger name="io.gatling.http.action.sse.fsm" level="DEBUG" />
```

## Example

Here's an example that runs against a stock market sample:

```scala
||< include-static "SseSample.scala#stock-market-sample" >||
```
