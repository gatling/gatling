---
title: "HTTP Helpers"
description: "Learn how to deal with cookies and cache"
lead: "Add and retrieve manually a cookie with Gatling"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Dealing with Cookies

Gatling supports cookies out-of-the-box and transparently, just like a browser would.

However, some use cases require a more fine grain control.

### Adding a Cookie

One might want to manually add or compute a cookie:

```scala
||< include-static "HttpHelperSample.scala#cookie" >||
```

Cookie can also take more optional parameters:

```scala
Cookie(name: Expression[String], value: Expression[String])
  .withDomain(domain: String)
  .withPath(path: String)
  .withMaxAge(maxAge: Int)
  .withSecure(secure: Boolean)
```

domain is optional, defaulting to base url domain

path is optional, defaulting to "/"

maxAge is optional, defaulting to `Long.MinValue`

secure is optional, defaulting to false

### Getting a Cookie Value {#getting-cookie-value}

Get the cookie value and put it in the session

```scala
||< include-static "HttpHelperSample.scala#getCookie" >||
```

CookieKey can also take more optional parameters:

```scala
CookieKey(name: Expression[String])
  .withDomain(domain: String)
  .withPath(path: String)
  .withSecure(secure: Boolean)
  .saveAs(key: String)
```

domain is optional, defaulting to base url domain

path is optional, defaulting to "/"

secure is optional, defaulting to false, means you only want secured cookies

saveAs is optional, defaulting to `name` param

### Flushing Session Cookies

One might want to simulate closing a browser, so Session cookies are dropped but permanent cookies are still there:

```scala
||< include-static "HttpHelperSample.scala#flushSessionCookies" >||
```

### Flushing All Cookies

One might want to flush the whole CookieJar:

```scala
||< include-static "HttpHelperSample.scala#flushCookieJar" >||
```

## Dealing with Caching

### Flushing the Cache

One might want to flush the whole HTTP cache (for the virtual user):

```scala
||< include-static "HttpHelperSample.scala#flushHttpCache" >||
```
