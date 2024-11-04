---
title: HTTP Helpers
seotitle: Gatling HTTP protocol reference - helpers
description: How to use built-in HTTP helpers to manipulate cookies and cache.
lead: Helpers for handling cookies and caches
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

## Dealing with cookies

Gatling supports cookies out-of-the-box and transparently, just like a browser would.

However, some use cases require a more fine grain control.

### Adding a cookie

One might want to manually add or compute a cookie:

{{< include-code "addCookie" >}}

Cookie can also take more optional parameters:

{{< include-code "cookie" >}}

* `domain` is optional, defaulting to base url domain
* `path` is optional, defaulting to "/"
* `maxAge` is and optional number of seconds, defaulting to `Long.MinValue`
* `secure` is optional, defaulting to false, meaning it's valid for both http and https urls

### Getting a cookie Value {#getting-cookie-value}

Get the cookie value and put it in the session

{{< include-code "getCookie" >}}

CookieKey can also take more optional parameters:

{{< include-code "cookieKey" >}}

* `domain` is optional. If undefined, defaults to the domain of the `baseUrl` defined in the `HttpProtocol`. In this case, fail if the `baseUrl` is undefined. Matching is based on [RFC6265's domain matching algorithm](https://datatracker.ietf.org/doc/html/rfc6265#section-5.1.3).
* `path` is optional. If defined, match based on [RFC6265's path matching algorithm](https://datatracker.ietf.org/doc/html/rfc6265#section-5.1.4). Otherwise, always match.
* `secure` is optional. If defined, match based on the cookie's secure attribute. Otherwise, always match.
* `saveAs` is optional, defaults to `name` param

### Flushing session cookies

Simulate closing a browser, so session cookies are dropped but not permanent cookies.

{{< include-code "flushSessionCookies" >}}

### Flushing all cookies

Flush the whole CookieJar.

{{< include-code "flushCookieJar" >}}

## Flushing the HTTP cache

Flush the virtual user's whole HTTP cache: known redirects, known expires and ETag.

{{< include-code "flushHttpCache" >}}
