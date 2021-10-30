---
title: "SSE (Server Sent Event)"
description: "SSE support is an extension to the HTTP DSL"
lead: "Learn the possible SSE operations with Gatling: connect, close"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 005090
---

SSE support is an extension to the HTTP DSL, whose entry point is the `sse(requestName: Expression[String])` method.

## `sseName`

If you want to deal with several SSE streams per virtual users, you have to give them a name and pass this name on each SSE operation:
For example:

{{< include-code "sseName" java scala >}}

Of course, this step is not required if you deal with one single SSE stream per virtual user.

## `connect`

The first thing is to get a server sent event:

For example:

{{< include-code "sseConnect" java scala >}}

{{< alert tip >}}
Gatling automatically sets `Accept` header to `text/event-stream` and `Cache-Control` to `no-cache`.
{{< /alert >}}

## `close`

Once you're done with a SSE stream, you can close it.

{{< include-code "sseClose" java scala >}}

## Checks

You deal with incoming messages with checks.

Beware of not missing messages that would be received prior to setting the check.

Gatling currently only supports blocking checks that will wait until receiving expected message or timing out.

### Set a Check

You can set a check right after connecting:

{{< include-code "check-from-connect" java scala >}}

Or you can set a check from main flow:

{{< include-code "check-from-flow" java scala >}}

You can set multiple checks sequentially. Each one will expect one single frame.

You can configure multiple checks in a single sequence:

{{< include-code "check-single-sequence" java scala >}}

You can also configure multiple check sequences with different timeouts:

{{< include-code "check-multiple-sequence" java scala >}}

### Create a check

You can create checks for server events with `checkMessage`.
You can use almost all the same check criteria as for HTTP requests.

{{< include-code "create-single-check" java scala >}}

You can have multiple criteria for a given message:

{{< include-code "create-multiple-checks" java scala >}}

### Matching messages

You can define `matching` criteria to filter messages you want to check.
Matching criterion is a standard check, except it doesn't take `saveAs`.
Non-matching messages will be ignored.

{{< include-code "check-matching" java scala >}}

## Debugging

You can inspect streams if you add the following logger to your logback configuration:

```xml
<logger name="io.gatling.http.action.sse.fsm" level="DEBUG" />
```

## Example

Here's an example that runs against a stock market sample:

{{< include-code "stock-market-sample" java scala >}}
