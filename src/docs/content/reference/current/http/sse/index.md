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

{{< include-code "SseSample.scala#sseName" scala >}}

Of course, this step is not required if you deal with one single SSE stream per virtual user.

### Connect

The first thing is to get a server sent event:

`connect(url: Expression[String])`

For example:

{{< include-code "SseSample.scala#sseConnect" scala >}}

{{< alert tip >}}
Gatling automatically sets `Accept` header to `text/event-stream` and `Cache-Control` to `no-cache`.
{{< /alert >}}

### Close

When you're done with a SSE stream, you can close it:

`close`

For example:

{{< include-code "SseSample.scala#sseClose" scala >}}

## Server Messages: Checks

You deal with incoming messages with checks.

Beware to not miss messages that would be received prior to setting the check.

Gatling currently only supports blocking checks that will waiting until receiving expected message or timing out.

### Set a Check

You can set a check right after connecting:

{{< include-code "SseSample.scala#check-from-connect" scala >}}

Or you can set a check from main flow:

{{< include-code "SseSample.scala#check-from-flow" scala >}}

You can set multiple checks sequentially. Each one will expect one single frame.

You can configure multiple checks in a single sequence:

{{< include-code "SseSample.scala#check-single-sequence" scala >}}

You can also configure multiple check sequences with different timeouts:

{{< include-code "SseSample.scala#check-multiple-sequence" scala >}}

### Create a check

You can create checks for server events with `checkMessage`.
You can use almost all the same check criteria as for HTTP requests.

{{< include-code "SseSample.scala#create-single-check" scala >}}

You can have multiple criteria for a given message:

{{< include-code "SseSample.scala#create-multiple-checks" scala >}}

### Matching messages

You can define `matching` criteria to filter messages you want to check.
Matching criterion is a standard check, except it doesn't take `saveAs`.
Non matching messages will be ignored.

{{< include-code "SseSample.scala#check-matching" scala >}}

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

{{< include-code "SseSample.scala#stock-market-sample" scala >}}
