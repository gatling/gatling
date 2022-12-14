---
title: "HTTP Helpers"
description: "How to use built-in HTTP helpers to manipulate cookies and cache."
lead: "Helpers for handling cookies and caches"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 2050500
---

## Dealing with Cookies

Gatling supports cookies out-of-the-box and transparently, just like a browser would.

However, some use cases require a more fine grain control.

### Adding a Cookie

One might want to manually add or compute a cookie:

{{< include-code "addCookie" java kt scala >}}

Cookie can also take more optional parameters:

{{< include-code "cookie" java kt scala >}}

* `domain` is optional, defaulting to base url domain
* `path` is optional, defaulting to "/"
* `maxAge` is and optional number of seconds, defaulting to `Long.MinValue`
* `secure` is optional, defaulting to false

### Getting a Cookie Value {#getting-cookie-value}

Get the cookie value and put it in the session

{{< include-code "getCookie" java kt scala >}}

CookieKey can also take more optional parameters:

{{< include-code "cookieKey" java kt scala >}}

* `domain` is optional, defaulting to base url domain
* `path` is optional, defaulting to "/"
* `secure` is optional, defaulting to false, means you only want secured cookies
* `saveAs` is optional, defaulting to `name` param

### Flushing Session Cookies

Simulate closing a browser, so session cookies are dropped but not permanent cookies.

{{< include-code "flushSessionCookies" java kt scala >}}

### Flushing All Cookies

Flush the whole CookieJar.

{{< include-code "flushCookieJar" java kt scala >}}

## Dealing with Caching

### Flushing the Cache

Flush the virtual user's whole HTTP cache.

{{< include-code "flushHttpCache" java kt scala >}}
