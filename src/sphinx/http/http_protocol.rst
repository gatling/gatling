:tocdepth: 2

.. _http-protocol:

#############
HTTP Protocol
#############

HTTP is the main protocol Gatling targets, so that's where we place most of our effort.

Gatling HTTP allows you to load test web applications, web services or websites.
It supports HTTP and HTTPS with almost every existing feature of common browsers such as caching, cookies, redirect, etc.

However, Gatling **is not a browser**: it won't run Javascript, won't apply CSS styles and trigger CSS background-images download, won't react to UI events, etc.
Gatling works at the HTTP protocol level.

Bootstrapping
=============

Use the ``http`` object in order to create an HTTP protocol.

As every protocol in Gatling, the HTTP protocol can be configured for a scenario.
This is done thanks to the following statements:

.. includecode:: code/HttpProtocolSample.scala#bootstrapping

Core parameters
===============

.. _http-protocol-base-url:

Base URL
--------

As you may have seen in the previous example, you can set a base URL.
This base URL will be prepended to all urls that does not start with ``http``, e.g.:

.. includecode:: code/HttpProtocolSample.scala#baseUrl

Load testing several servers with client based load balancing
-------------------------------------------------------------

If you want to load test several servers at the same time, to bypass a load-balancer for example, you can use methods named ``baseUrls`` which accepts a ``String*`` or a ``List[String]``:

.. includecode:: code/HttpProtocolSample.scala#baseUrls

Each virtual user will pick one of the baseUrl from the list once and for all when it starts, based on a round-robin strategy.

.. _http-protocol-warmup:

Automatic warm up
-----------------

The Java/NIO engine start up introduces an overhead on the first request to be executed.
In order to compensate this effect, Gatling automatically performs a request to https://gatling.io.

To disable this feature, just add ``.disableWarmUp`` to an HTTP Protocol Configuration definition.
To change the warm up url, just add ``.warmUp("newUrl")``.

.. includecode:: code/HttpProtocolSample.scala#warmUp

Engine parameters
=================

.. _http-protocol-max-connection:

Max connection per host
-----------------------

In order to mimic real web browser, Gatling can run multiple concurrent connections **per virtual user** when fetching resources on the same hosts.
By default, Gatling caps the number of concurrent connections per remote host per virtual user to 6, but you can change this number with ``maxConnectionsPerHost(max: Int)``.

Gatling ships a bunch of built-ins for well-known browsers:

* ``maxConnectionsPerHostLikeFirefoxOld``
* ``maxConnectionsPerHostLikeFirefox``
* ``maxConnectionsPerHostLikeOperaOld``
* ``maxConnectionsPerHostLikeOpera``
* ``maxConnectionsPerHostLikeSafariOld``
* ``maxConnectionsPerHostLikeSafari``
* ``maxConnectionsPerHostLikeIE7``
* ``maxConnectionsPerHostLikeIE8``
* ``maxConnectionsPerHostLikeIE10``
* ``maxConnectionsPerHostLikeChrome``

.. includecode:: code/HttpProtocolSample.scala#maxConnectionsPerHost

.. _http-protocol-connection-sharing:

Connection Sharing
------------------

The default behavior is that every virtual user has its own connection pool and its own SSLContext.
This behavior meets your needs when you want to simulate internet traffic where each virtual user simulates a web browser.

Instead, if you want to simulate server to server traffic where the actual client has a long lived connection pool, you want to have the virtual users share a single global connection pool.
You can achieve this behavior with the ``.shareConnections`` param.

.. _http-protocol-http2:

HTTP/2 Support
--------------

HTTP/2 experimental support can be enabled with the `.enableHttp2` option.

Note that you'll either need your injectors to run with Java 9+, or make sure that `gatling.http.ahc.useOpenSsl` wasn't turned to `false` in Gatling configuration.

.. includecode:: code/HttpProtocolSample.scala#enableHttp2

.. warning::
HTTP/2 Push is currently not supported.

When HTTP/2 is enabled Gatling will try to connect to your remotes using HTTP/2 through the ALPN protocol.
If your remote supports HTTP/2, Gatling will use the protocol, and fall back to HTTP/1 otherwise. There is no specific code to add in the middle of your requests.

Next time you use that remote with the same user, if Gatling knows that your remote doesn't support HTTP/2, it won't try again and therefore won't use ALPN.

One of the main purpose of HTTP/2 is to support multiplexing. This means that on a single connection, you are able to send multiple requests, without waiting for the responses,
and receive these responses in whatever order.
It means that, using HTTP/2, browsers and Gatling won't open additional connections to the same remote for a given virtual user (assuming you don't enable `shareConnections``) once they know that the remote is using HTTP/2.
The first time Gatling encounters a remote, the connections will be opened like in HTTP/1 mode if there are multiple requests (for example in a ``resources`` statement).
If the remote is using HTTP/1, these connections will be used if needed. If it is using HTTP/2, a single connection will be maintained, and the other ones will reach idle timeout and be closed.

It is possible to populate the Gatling cache concerning protocol and remotes before the run, using the ``http2PriorKnowledgeMap(Map[String, Boolean])`` method on the protocol.

.. includecode:: code/HttpProtocolSample.scala#http2PriorKnowledgeMap

With this method, you are able to tell Gatling that remotes support HTTP/2 or not.
It means that if you are setting a remote to true (it supports HTTP/2), additional connections won't be created the first time the remote is encountered in the simulation.
If you are setting a remote to false (it doesn't support HTTP/2), ALPN won't be used, and additional connections will be created.

This option is useful to simulate users that already went to your website, and whose browsers already cached the fact that your website is using HTTP/2 or HTTP/1.

.. warning::
If you configure a remote in prior knowledge and set it to true, but that the ALPN ends in the remote only supporting HTTP/1, the request will crash.
  Use the ``http2PriorKnowledge`` option only if you are sure about your remote configuration.

.. _http-protocol-dns:

DNS Name Resolution
-------------------

By default, Gatling uses Java's DNS name resolution. This cache has a TTL of 30s by default on OpenJDK and doesn't honor the DNS records' own TTL.
You can control the TTL with ``-Dsun.net.inetaddr.ttl=N`` where `N` is a number of seconds.
Please note the ``sun.net.inetaddr.ttl`` System property is deprecated and one should use the ``networkaddress.cache.ttl`` Security property instead, see `doc <https://docs.oracle.com/javase/8/docs/technotes/guides/net/properties.html>`_.

If you're using the Java DNS name resolution and have multiple IP (multiple DNS records) for a given hostname, Gatling will automatically shuffle them
to emulate DNS round-robin.

You can use a Netty based DNS resolution instead, with ``.asyncNameResolution()``.
This method can take a sequence of DNS server adresses, eg ``.asyncNameResolution("8.8.8.8")``.
If you don't pass DNS servers, Gatling will use the ones from your OS configuration on Linux and MacOS only,
and to Google's ones on Windows(don't run with heavy load as Google will block you).

You can also make it so that every virtual user performs its own DNS name resolution with ``.perUserNameResolution``.
This parameter is only effective when using ``asyncNameResolution``.

.. _http-protocol-hostname-aliasing:

Hostname Aliasing
-----------------

You can of course define hostname aliases at the OS level in the ``/etc/hosts`` file.

But you can use ``.hostNameAliases`` to pass aliases programmatically:

.. includecode:: code/HttpProtocolSample.scala#hostNameAliases

.. _http-protocol-virtual-host:

Virtual Host
------------

One can set a different Host than the url one::

  virtualHost(virtualHost: Expression[String])

.. _http-protocol-local-address:

Local address
-------------

You can bind the sockets from specific local addresses instead of the default one::

  localAddress(localAddress: String)
  localAddresses(localAddress1: String, localAddress2: String)
  useAllLocalAddresses // automatically discover all bindable local addresses
  useAllLocalAddressesMatching(regex1, regex2) // automatically discover all bindable local addresses matching one of the pattern parameters (String)

When setting multiple addresses, each virtual user is assigned to one single local address once and for all.

.. _http-protocol-kmf:

KeyManagerFactory
-----------------

By default, Gatling uses the KeyManagerFactory configuration defined in `gatling.conf`, or if undefined, falls back to the JVM's default one.

Then, it's possible to have per virtual user KeyManagerFactories, typically if you want them to use different sets of keys::

    perUserKeyManagerFactory(f: Long => KeyManagerFactory)

This function's input is the virtual user's id (if you need it to generate some file name) and returns a `javax.net.ssl.KeyManagerFactory <https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/KeyManagerFactory.html>`_.

Request building parameters
===========================

.. _http-protocol-referer:

Automatic Referer
-----------------

The ``Referer`` HTTP header can be automatically computed.
This feature is enabled by default.

To disable this feature, just add ``.disableAutoReferer`` to an HTTP Protocol Configuration definition.

.. _http-protocol-caching:

Caching
-------

Gatling caches responses using :

* Expires header
* Cache-Control header
* Last-Modified header
* ETag

To disable this feature, just add ``.disableCaching`` to an HTTP Protocol Configuration definition.

.. note:: When a response gets cached, checks are disabled.

.. _http-protocol-urlencoding:

Url Encoding
------------

Url components are supposed to be `urlencoded <http://www.w3schools.com/tags/ref_urlencode.asp>`_.
Gatling will encode them for you, there might be some corner cases where already encoded components might be encoded twice.

If you know that your urls are already properly encoded, you can disable this feature with ``.disableUrlEncoding``.
Note that this feature can also be :ref:`disabled per request <http-request-urlencoding>`.

.. _http-protocol-silencing:

Silencing
---------

Request stats are logged and then used to produce reports.
Sometimes, some requests may be important for you for generating load, but you don't actually want to report them.
Typically, reporting all static resources might generate a lot of noise, and yet failed static resources might not be blocking from a user experience perspective.

Gatling provides several means to turn requests silent.
Silent requests won't be reported and won't influence error triggers such as :ref:`tryMax <scenario-trymax>` and :ref:`exitHereIfFailed <scenario-exithereiffailed>`.
Yet, response times will be accounted for in ``group`` times.

Some parameters are available here at protocol level, some others are available at request level.

Rules are:

* explicitly turning a given request :ref:`silent <http-request-silent>` or :ref:`notSilent <http-request-notsilent>` has precedence over everything else
* otherwise, a request is silent if it matches protocol's ``silentUri`` filter
* otherwise, a request is silent if it's a resource (not a top level request) and protocol's ``silentResources`` flag has been turned on
* otherwise, a request is not silent

.. _http-protocol-silentUri:

``silentUri`` lets you pass a regular expression that would disable logging for ALL matching requests:

.. includecode:: code/HttpProtocolSample.scala#silentUri

.. _http-protocol-silentResources:

``silentResources`` silences all resource requests, except the ones that were explicitly turned ``notSilent``.

.. _http-protocol-headers:

HTTP Headers
------------

Gatling lets you set some generic headers at the http protocol definition level with:

* ``header(name: String, value: Expression[String])``: set a single header.
* ``headers(headers: Map[String, String])``: set a bunch of headers.

e.g.:

.. includecode:: code/HttpProtocolSample.scala#headers

You have also the following built-ins for the more commons headers:

* ``acceptHeader(value: Expression[String])``: set ``Accept`` header.
* ``acceptCharsetHeader(value: Expression[String])``: set ``Accept-Charset`` header.
* ``acceptEncodingHeader(value: Expression[String])``: set ``Accept-Encoding`` header.
* ``acceptLanguageHeader(value: Expression[String])``: set ``Accept-Language`` header.
* ``authorizationHeader(value: Expression[String])``: set ``Authorization`` header.
* ``connectionHeader(value: Expression[String])``: set ``Connection`` header.
* ``contentTypeHeader(value: Expression[String])``: set ``Content-Type`` header.
* ``doNotTrackHeader(value: Expression[String])``: set ``DNT`` header.
* ``userAgentHeader(value: Expression[String])``: set ``User-Agent`` header.

.. _http-protocol-signature:

Signature Calculator
--------------------

You can set a function to sign a request once Gatling has built it, just before it's being sent over the wire:

``sign(calculator: Expression[SignatureCalculator])``

We also provide a built-in for OAuth1:

``signWithOAuth1(consumerKey: Expression[String], clientSharedSecret: Expression[String], token: Expression[String], tokenSecret: Expression[String])``

.. note:: For more details see the dedicated section :ref:`here <http-request-signature>`.

.. _http-protocol-auth:

Authentication
--------------

You can set the authentication methods at protocol level with these methods:

* ``basicAuth(username: Expression[String], password: Expression[String])``
* ``digestAuth(username: Expression[String], password: Expression[String])``

.. note:: For more details see the dedicated section :ref:`here <http-request-authentication>`.

Response handling parameters
============================

.. _http-protocol-redirect:

Follow redirects
----------------

By default Gatling automatically follow redirects in case of 301, 302, 303, 307 or 308 response status code, you can disable this behavior with ``.disableFollowRedirect``.

To avoid infinite redirection loops, Gatling sets a limit on the number of redirects.
The default value is 20. You can tune this limit with: ``.maxRedirects(max: Int)``

By default, Gatling will change the method to "GET" on 302 to conform to most user agents' behavior.
You can disable this behavior with ``.strict302Handling``.

.. _http-protocol-response-transformer:

Response Transformers
---------------------

Some people might want to process manually the response. Gatling protocol provides a hook for that need: ``transformResponse(responseTransformer: ResponseTransformer)``

.. note:: For more details see the dedicated section :ref:`here <http-response-transformer>`.

.. _http-protocol-check:

Checks
------

You can define checks at the http protocol definition level with: ``check(checks: HttpCheck*)``.
They will be apply on all the requests, however you can disable them for given request thanks to the ``ignoreProtocolChecks`` method.

.. note:: For more details see the dedicated section :ref:`here <http-check>`.

.. _http-protocol-infer:

Resource inferring
------------------

Gatling can fetch resources in parallel in order to emulate the behavior of a real web browser.

At the protocol level, you can use ``inferHtmlResources`` methods, so Gatling will automatically parse HTML to find embedded resources and load them asynchronously.

The supported resources are:

* ``<script>``
* ``<base>``
* ``<link>``
* ``<bgsound>``
* ``<frame>``
* ``<iframe>``
* ``<img>``
* ``<input>``
* ``<body>``
* ``<applet>``
* ``<embed>``
* ``<object>``
* import directives in HTML and @import CSS rule.

Other resources are not supported: css images, javascript triggered resources, conditional comments, etc.

You can also specify black/white list or custom filters to have a more fine grain control on resource fetching.
``WhiteList`` and ``BlackList`` take a sequence of pattern, eg ``Seq("http://www.google.com/.*", "http://www.github.com/.*")``, to include and exclude respectively.

* ``inferHtmlResources(white: WhiteList)``: fetch all resources matching a pattern in the white list.
* ``inferHtmlResources(white: WhiteList, black: BlackList)``: fetch all resources matching a pattern in the white list excepting those in the black list.
* ``inferHtmlResources(black: BlackList)``: fetch all resources excepting those matching a pattern in the black list.
* ``inferHtmlResources(black: BlackList, white: WhiteList)``: fetch all resources excepting those matching a pattern in the black list and not in the white list.
* ``inferHtmlResources(filters: Option[Filters])``

Finally, you can specify the strategy for naming those requests in the reports:

* ``nameInferredHtmlResourcesAfterUrlTail``(default): name requests after the resource's url tail (after last ``/``)
* ``nameInferredHtmlResourcesAfterPath``: name requests after the resource's path
* ``nameInferredHtmlResourcesAfterAbsoluteUrl``: name requests after the resource's absolute url
* ``nameInferredHtmlResourcesAfterRelativeUrl``: name requests after the resource's relative url
* ``nameInferredHtmlResourcesAfterLastPathElement``: name requests after the resource's last path element
* ``nameInferredHtmlResources(f: Uri => String)``: name requests with a custom strategy

.. _http-protocol-proxy:

Proxy parameters
----------------

You can tell Gatling to use a proxy to send the HTTP requests.
You can optionally set a different port for HTTPS and credentials:

.. includecode:: code/HttpProtocolSample.scala#proxy

You can also disable the use of proxy for a given list of hosts with ``noProxyFor(hosts: String*)``:

.. includecode:: code/HttpProtocolSample.scala#noProxyFor
