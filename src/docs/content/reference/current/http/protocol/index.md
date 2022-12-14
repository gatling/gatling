---
title: "HTTP Protocol"
description: "How to configure Gatling HTTP behavior such as baseUrl, common HTTP headers, common HTTP checks and connection pool sharing."
lead: "Learn about connection, redirect, caching, resource infering and dns resolution"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 2050100
---

HTTP is the main protocol Gatling targets, so that's where we place most of our effort.

Gatling HTTP allows you to load test web applications, web services or websites.
It supports HTTP and HTTPS with almost every existing feature of common browsers such as caching, cookies, redirect, etc.

However, Gatling **is not a browser**: it won't run Javascript, won't apply CSS styles and trigger CSS background-images download, won't react to UI events, etc.
Gatling works at the HTTP protocol level.

## Bootstrapping

Use the `http` object in order to create an HTTP protocol.

As every protocol in Gatling, the HTTP protocol can be configured for a scenario.
This is done thanks to the following statements:

{{< include-code "bootstrapping" java kt scala >}}

## HTTP Engine

#### `warmUp`

The Java/NIO engine start up introduces an overhead on the first request to be executed.
In order to compensate this effect, Gatling automatically performs a request to https://gatling.io.

{{< include-code "warmUp" java kt scala >}}

#### `maxConnectionsPerHost`

In order to mimic real web browser, Gatling can run multiple concurrent connections **per virtual user** when fetching resources on the same hosts over HTTP/1.1.
By default, Gatling caps the number of concurrent connections per remote host per virtual user to 6, which makes sense for modern browsers.
You can change this number with `maxConnectionsPerHost(max: Int)`.

{{< include-code "maxConnectionsPerHost" java kt scala >}}

#### `shareConnections`

The default behavior is that every virtual user has its own connection pool and its own SSLContext.
This behavior meets your needs when you want to simulate internet traffic where each virtual user simulates a web browser.

Instead, if you want to simulate server to server traffic where the actual client has a long-lived connection pool, you want to have the virtual users share a single global connection pool.

{{< include-code "shareConnections" java kt scala >}}

#### `enableHttp2`

HTTP/2 experimental support can be enabled with the `.enableHttp2` option.

Note that you'll either need your injectors to run with Java 9+, or make sure that `gatling.http.ahc.useOpenSsl` wasn't turned to `false` in Gatling configuration.

{{< include-code "enableHttp2" java kt scala >}}

{{< alert warning >}}
HTTP/2 Push is not supported.
{{< /alert >}}

When HTTP/2 is enabled, Gatling will try to connect to your remotes using HTTP/2 through the ALPN protocol.
If your remote supports HTTP/2, Gatling will use the protocol, and fall back to HTTP/1 otherwise. There is no specific code to add in the middle of your requests.

Next time you use that remote with the same user, if Gatling knows that your remote doesn't support HTTP/2, it won't try again and therefore won't use ALPN.

One of the main purpose of HTTP/2 is to support multiplexing. This means that on a single connection, you are able to send multiple requests, without waiting for the responses,
and receive these responses in whatever order.
It means that, using HTTP/2, browsers and Gatling won't open additional connections to the same remote for a given virtual user (assuming you don't enable `shareConnections`) once they know that the remote is using HTTP/2.
The first time Gatling encounters a remote, the connections will be opened like in HTTP/1 mode if there are multiple requests (for example in a `resources` statement).
If the remote is using HTTP/1, these connections will be used if needed. If it is using HTTP/2, a single connection will be maintained, and the other ones will reach idle timeout and be closed.

You can configure which remotes support HTTP/2 or not.
It means that if you are setting a remote to true (it supports HTTP/2), additional connections won't be created the first time the remote is encountered in the simulation.
If you are setting a remote to false (it doesn't support HTTP/2), ALPN won't be used, and additional connections will be created.

This option is useful to simulate users that already went to your website, and whose browsers already cached the fact that your website is using HTTP/2 or HTTP/1.

{{< include-code "http2PriorKnowledge" java kt scala >}}

{{< alert warning >}}
If you configure a remote in prior knowledge and set it to true, but that the ALPN ends in the remote only supporting HTTP/1, the request will crash.

Use this option only if you are sure about your remote configuration.
{{< /alert >}}

#### `JDK Blocking Resolver (default)`

By default, Gatling uses Java's DNS name resolution. This cache has a TTL of 30s by default on OpenJDK and doesn't honor the DNS records' own TTL.
You can control the TTL with:
* the now deprecated `sun.net.inetaddr.ttl` System property: `-Dsun.net.inetaddr.ttl=N` where `N` is a number of seconds
* the now recommended `networkaddress.cache.ttl` Security property, see [reference here](https://docs.oracle.com/en/java/javase/17/docs/api/system-properties.html).

If you're using the Java DNS name resolution and have multiple IP (multiple DNS records) for a given hostname, Gatling will automatically shuffle them
to emulate DNS round-robin.

#### `asyncNameResolution`

You can use Gatling's own async DNS resolver instead, with `.asyncNameResolution()`.

{{< include-code "dns-async" java kt scala >}}

#### `hostNameAliases`

You can of course define hostname aliases at the OS level in the `/etc/hosts` file.

But you can use pass aliases programmatically.

{{< include-code "hostNameAliases" java kt scala >}}

#### `virtualHost`

You can change the `Host` to something else than the url domain.

{{< include-code "virtualHost" java kt scala >}}

#### `localAddress`

It's possible to have multiple IP addresses for your load generators, typically using [IP-aliasing or `iproute2`](https://www.kernel.org/doc/html/v5.8/networking/alias.html).

In this case, you can bind the sockets from specific local addresses instead of the default one:

{{< include-code "localAddress" java kt scala >}}

Note that when setting multiple addresses, each virtual user is assigned to one single local address once and for all in a round-robin fashion.

{{< alert warning >}}
Some tools have been misnaming this feature as IP spoofing. This is plain wrong. [IP spoofing](https://www.cloudflare.com/en-gb/learning/ddos/glossary/ip-spoofing/) is a way to generate malicious traffic, which is something Gatling will never do. Anyway, if by "IP spoofing" you mean "having the load generator use multiple source IP addresses", this is the feature you're looking for.
{{< /alert >}}

#### `perUserKeyManagerFactory`

By default, Gatling uses the KeyManagerFactory configuration defined in `gatling.conf`, or if undefined, falls back to the JVM's default one.

Then, it's possible to have per virtual user KeyManagerFactories, typically if you want them to use different sets of keys.

{{< include-code "perUserKeyManagerFactory" java kt scala >}}

This function's input is the virtual user's id (if you need it to generate some file's name) and returns a [javax.net.ssl.KeyManagerFactory](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/KeyManagerFactory.html).

## Request Generation

#### `baseUrl`

As you may have seen in the previous example, you can set a base URL.
This base URL will be prepended to all urls that does not start with `http`, e.g.:

{{< include-code "baseUrl" java kt scala >}}

#### `baseUrls`

You might want to load test several servers at the same time, to bypass a load-balancer for example.

{{< include-code "baseUrls" java kt scala >}}

Each virtual user will pick one of the baseUrls from the list once and for all when it starts, based on a round-robin strategy.

#### `disableAutoReferer`

By default, Gatling automatically computes the `Referer` HTTP header from the request history.
You can disable this feature.

{{< include-code "disableAutoReferer" java kt scala >}}

#### `disableCaching`

By default, Gatling caches responses based on:

* Expires header
* Cache-Control header
* Last-Modified header
* ETag

You can disable this feature.

{{< include-code "disableCaching" java kt scala >}}

{{< alert tip >}}
When a response gets cached, checks are disabled.
{{< /alert >}}

#### `disableUrlEncoding`

Url components are supposed to be [urlencoded](http://www.w3schools.com/tags/ref_urlencode.asp).
By default, Gatling will encode them for you.

If you know that your urls are already properly encoded and want to optimize performance, you can disable this feature.

{{< include-code "disableUrlEncoding" java kt scala >}}

Note that this feature can also be [disabled per request]({{< ref "../request#url-encoding" >}}).

#### `silentUri`

Request stats are logged and then used to produce reports.
Sometimes, some requests may be important for you for generating load, but you don't actually want to report them.
Typically, reporting all static resources might generate a lot of noise, and yet failed static resources might not be blocking from a user experience perspective.

Gatling provides several means to turn requests silent.
Silent requests won't be reported and won't influence error triggers such as [tryMax]({{< ref "../../core/scenario#trymax" >}}) and [exitHereIfFailed]({{< ref "../../core/scenario#exithereiffailed" >}}).
Yet, response times will be accounted for in `group` times.

Some parameters are available here at protocol level, some others are available at request level.

Rules are:
* explicitly turning a given request [silent]({{< ref "../request#silencing" >}}) or [notSilent]({{< ref "../request#silencing" >}}) has precedence over everything else
* otherwise, a request is silent if it matches protocol's `silentUri` filter
* otherwise, a request is silent if it's a resource (not a top level request) and protocol's `silentResources` flag has been turned on
* otherwise, a request is not silent

{{< include-code "silentUri" java kt scala >}}

#### `header`

Gatling lets you define some [HTTP headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers) to be set one all requests.

{{< include-code "headers" java kt scala >}}

You have also the following built-ins for the more commons headers.

{{< include-code "headers-built-ins" java kt scala >}}

#### `sign`

You can set a function to sign a request once Gatling has built it, just before it's being sent over the wire.
Typically, you would compute an extra HTTP header.

{{< include-code "sign" java kt scala >}}

We also provide a built-in for OAuth1.

{{< include-code "sign-oauth1" java kt scala >}}

#### `basicAuth` and `digestAuth` {#authorization}

You can set the authentication methods at protocol level to be applied to all requests.

{{< include-code "authorization" java kt scala >}}

## Response Handling

#### `disableFollowRedirect`

By default, Gatling automatically follow redirects in case of 301, 302, 303, 307 or 308 response status code.
You can disable this behavior.

{{< include-code "disableFollowRedirect" java kt scala >}}

To avoid infinite redirection loops, Gatling sets a limit on the number of redirects.
The default value is 20. You can tune this limit with: `.maxRedirects(int)`

By default, Gatling will change the method to "GET" on 302 to conform to most user agents' behavior.
You can disable this behavior and keep the original method with `.strict302Handling`.

#### `redirectNamingStrategy`

By default, Gatling will generate redirected request names such as "<ORIGINAL_REQUEST_NAME> Redirect <REDIRECT_COUNT>".
You can define your own custom strategy.

The function takes 3 parameters
* `uri` the target Location, of type `io.gatling.http.client.uri.Uri`
* `originalRequestName`
* `redirectCount`

{{< include-code "redirectNamingStrategy" java kt scala >}}

#### `transformResponse`

You might want to process manually the response.

{{< include-code "transformResponse" java kt scala >}}

{{< alert tip >}}
For more details see the dedicated section [here]({{< ref "../request#response-transformers" >}}).
{{< /alert >}}

#### `check`

You can define checks at the http protocol definition level with: `check(checks: HttpCheck*)`.
They will be applied on all the requests, however you can disable them for given request thanks to the `ignoreProtocolChecks` method.

{{< alert tip >}}
For more details see the dedicated section [here]({{< ref "../check" >}}).
{{< /alert >}}

#### `inferHtmlResources`

Gatling can fetch resources in parallel in order to emulate the behavior of a real web browser.

At the protocol level, you can use `inferHtmlResources` methods, so Gatling will automatically parse HTML to find embedded resources and load them asynchronously.

The supported resources are:

* `<script>`
* `<base>`
* `<link>`
* `<bgsound>`
* `<frame>`
* `<iframe>`
* `<img>`
* `<input>`
* `<body>`
* `<applet>`
* `<embed>`
* `<object>`
* import directives in HTML and @import CSS rule.

Other resources are not supported: css images, javascript triggered resources, conditional comments, etc.

You can also specify `AllowList` and `DenyList` based on Java Regex patterns to have a more fine grain control on resource fetching.

{{< include-code "inferHtmlResources" java kt scala >}}

#### `nameInferredHtmlResources`

By default, Gatling will generate resource request names automatically.
You can control this behavior and even define your own custom strategy.

{{< include-code "nameInferredHtmlResources" java kt scala >}}

## Proxy

You can tell Gatling to use a proxy to send the HTTP requests.
You can optionally set a different port for HTTPS and credentials:

{{< include-code "proxy" java kt scala >}}

You can also disable the use of proxy for some hosts with `noProxyFor`:

{{< include-code "noProxyFor" java kt scala >}}
