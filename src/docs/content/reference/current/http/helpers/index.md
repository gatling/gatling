---
title: "HTTP Helpers"
description: "Learn how to deal with cookies and cache"
lead: "Add and retrieve manually a cookie with Gatling"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 005050
---

## Dealing with Cookies

Gatling supports cookies out-of-the-box and transparently, just like a browser would.

However, some use cases require a more fine grain control.

### Adding a Cookie

One might want to manually add or compute a cookie:

{{< include-code "addCookie" java scala >}}

Cookie can also take more optional parameters:

{{< include-code "cookie" java scala >}}

* *domain* is optional, defaulting to base url domain
* *path* is optional, defaulting to "/"
* *maxAge* is and optional number of seconds, defaulting to `Long.MinValue`
* *secure* is optional, defaulting to false

### Getting a Cookie Value {#getting-cookie-value}

Get the cookie value and put it in the session

{{< include-code "getCookie" java scala >}}

CookieKey can also take more optional parameters:

{{< include-code "cookieKey" java scala >}}

* *domain* is optional, defaulting to base url domain
* *path* is optional, defaulting to "/"
* *secure* is optional, defaulting to false, means you only want secured cookies
* *saveAs* is optional, defaulting to `name` param

### Flushing Session Cookies

Simulate closing a browser, so session cookies are dropped but not permanent cookies.

{{< include-code "flushSessionCookies" java scala >}}

### Flushing All Cookies

Flush the whole CookieJar.

{{< include-code "flushCookieJar" java scala >}}

## Dealing with Caching

### Flushing the Cache

Flush the virtual user's whole HTTP cache.

{{< include-code "flushHttpCache" java scala >}}
