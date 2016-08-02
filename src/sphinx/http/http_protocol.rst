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

.. includecode:: code/HttpProtocol.scala#bootstrapping

Core parameters
===============

.. _http-protocol-base-url:

Base URL
--------

As you may have seen in the previous example, you can set a base URL.
This base URL will be prepended to all urls that does not start with ``http``, e.g.:

.. includecode:: code/HttpProtocol.scala#baseUrl

Load testing several servers with client based load balancing
-------------------------------------------------------------

If you want to load test several servers at the same time, to bypass a load-balancer for example, you can use methods named ``baseURLs`` which accepts a ``String*`` or a ``List[String]``:

.. includecode:: code/HttpProtocol.scala#baseUrls

The selection of the URL is made at each request, using the ``Random`` generator.


.. _http-protocol-warmup:

Automatic warm up
-----------------

The Java/NIO engine start up introduces an overhead on the first request to be executed.
In order to compensate this effect, Gatling automatically performs a request to http://gatling.io.

To disable this feature, just add ``.disableWarmUp`` to an HTTP Protocol Configuration definition.
To change the warm up url, just add ``.warmUp("newUrl")``.

.. includecode:: code/HttpProtocol.scala#warmUp

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

.. includecode:: code/HttpProtocol.scala#maxConnectionsPerHost

.. _http-protocol-connection-sharing:

Connection Sharing
------------------

In Gatling 1, connections are shared amongst users until 1.5 version.
This behavior does not match real browsers, and doesn't support SSL session tracking.

In Gatling 2, the default behavior is that every user has his own connection pool.
This can be tuned with the ``.shareConnections`` param.

.. _http-protocol-client-sharing:

HTTP Client Sharing
-------------------

If you need more isolation of your user, for instance if you need a dedicated key store per user,
Gatling lets you have an instance of the HTTP client per user with ``.disableClientSharing``.

.. _http-protocol-hostname-resolution:

Hostname Resolution
-------------------

By default, Gatling uses Java's name resolution, meaning that it uses a cache shared amongst all virtual users.
This cache can be disabled with ``-Dsun.net.inetaddr.ttl=0``.

One can change this behaviour and set ``.perUserNameResolution`` so all virtual users resolve names on their own.
This feature is experimental, but might become the default in future versions.

This feature is pretty useful if you're dealing with an elastic cluster where new IPs are added to the DNS server under load,
for example with AWS Elastic Load Balancer (ELB).

.. _http-protocol-hostname-aliasing:

Hostname Aliasing
-----------------

You can of course define hostname aliases at the OS level in the ``/etc/hosts`` file.

But you can also pass a ``Map[String, String]`` to ``.hostNameAliases`` where values are valid IP addresses.
Note that, just like with ``/etc/hosts`` you can only define one IP per alias.

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

When setting multiple addresses, each virtual user is assigned to one single local address once and for all.

Request building parameters
===========================

.. _http-protocol-referer:

Automatic Referer
-----------------

The ``Referer`` HTTP header can be automatically computed.
This feature is enabled by default.

To disable this feature, just add ``.disableAutomaticReferer`` to an HTTP Protocol Configuration definition.

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
Typically, reporting all static resources might generate a lot of noise, and yet failed static resources are usually non blocking from a user experience perspective.

Gatling provides several means to turn requests silent.
Silent requests won't be reported and won't influence error triggers such as :ref:`tryMax <scenario-trymax>` and :ref:`exitHereIfFailed <scenario-exithereiffailed>`.
Yet, response times will be accounted for in ``group`` times.

Some parameters are available here at protocol level, some others are available at request level.

Rules are:

* explicitly turning a given request :ref:`silent <http-request-silent>` or :ref:`notSilent <http-request-notsilent>` has precedence over everything else
* otherwise, a request is silent if it matches protocol's ``silentURI`` filter
* otherwise, a request is silent if it's a resource (not a top level request) and protocol's ``silentResources`` flag has been turned on
* otherwise, a request is not silent

.. _http-protocol-silentURI:

``silentURI`` lets you pass a regular expression that would disable logging for ALL matching requests:

.. includecode:: code/HttpProtocol.scala#silentURI

.. _http-protocol-silentResources:

``silentResources`` silences all resource requests, except the ones that were explicitly turned ``notSilent``.

.. _http-protocol-headers:

HTTP Headers
------------

Gatling lets you set some generic headers at the http protocol definition level with:

* ``header(name: String, value: Expression[String])``: set a single header.
* ``headers(headers: Map[String, String])``: set a bunch of headers.

e.g.:

.. includecode:: code/HttpProtocol.scala#headers

.. warning:: ``headers`` used to be named ``baseHeaders``. Old name was deprecated, then removed in 2.1.

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

.. _http-protocol-auth:

Authentication
--------------

You can set the authentication methods at protocol level with these methods:

* ``basicAuth(username: Expression[String], password: Expression[String])``
* ``digestAuth(username: Expression[String], password: Expression[String])``
* ``ntlmAuth(username: Expression[String], password: Expression[String], ntlmDomain: Expression[String], ntlmHost: Expression[String])``
* ``authRealm(realm: Expression[com.ning.http.client.Realm])``

.. note:: For more details see the dedicated section :ref:`here <http-request-authentication>`.

Response handling parameters
============================

.. _http-protocol-redirect:

Follow redirects
----------------

By default Gatling automatically follow redirects in case of 301, 302, 303 or 307 response status code, you can disable this behavior with ``.disableFollowRedirect``.

To avoid infinite redirection loops, you can specify a number max of redirects with:  ``.maxRedirects(max: Int)``

By default, Gatling will change the method to "GET" on 302 to conform to most user agents' behavior.
You can disable this behavior with ``.strict302Handling``.

.. _http-protocol-chunksdiscard:

Response chunks discarding
--------------------------

Beware that, as an optimization, Gatling doesn't keep response chunks unless a check is defined on the response body or that debug logging is enabled.
However some people might want to always keep the response chunks, thus you can disable the default behavior with ``disableResponseChunksDiscarding``.

.. _http-protocol-extractor:

Dumping custom data
-------------------

Some people might want more data than what Gatling normally dumps in the ``simulation.log`` file.

Http protocol provide a hook for dumping extra data with ``extraInfoExtractor(f: ExtraInfoExtractor)``.
``ExtraInfoExtractor`` is a shortcut for the function type: ``(ExtraInfo) => List[Any]``.
Thus your extractor need to return a ``List[Any]``, ``Any`` is the equivalent of ``Object`` in Scala.
``ExtraInfo`` gives you access to :

* ``requestName``: The name of the request.
* ``status``: The status of the request, i.e. OK/KO.
* ``session``: The user's Session.
* ``request``: The http request.
* ``response``: The http response.

The extra data will be appended to the relative records in the ``simulation.log`` file and reports generation will ignore them.
It's up to the user to build his own analysis system for them.

For example, it you'd like the dump the response body's length to ``simulation.log``, you would do::

  val httpProtocol = http.extraInfoExtractor(extraInfo => List(extraInfo.response.bodyLength))

Gatling provides a built-in ``ExtraInfoExtractor``, ``dumpSessionOnFailure``, which dumps the user's session to ``simulation.log`` if the request failed.

.. _http-protocol-response-transformer:

Response Transformers
---------------------

Some people might want to process manually the response. Gatling protocol provides a hook for that need: ``transformResponse(responseTransformer: ResponseTransformer)``

.. note:: For more details see the dedicated section :ref:`here <http-response-transformer>`.

.. _http-protocol-check:

Checks
------

You can define checks at the http protocol definition level with: ``check(checks: HttpCheck*)``.
They will be apply on all the requests, however you can disable them for given request thanks to the ``ignoreDefaultChecks`` method.

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
* ``inferHtmlResources(black: BlackList, white: WhiteList = WhiteList(Nil))``: fetch all resources excepting those matching a pattern in the black list and not in the white list.
* ``inferHtmlResources(filters: Option[Filters])``

.. _http-protocol-proxy:

Proxy parameters
----------------

You can tell Gatling to use a proxy to send the HTTP requests.
You can optionally set a different port for HTTPS and credentials:

.. includecode:: code/HttpProtocol.scala#proxy

You can also disable the use of proxy for a given list of host with ``noProxyFor(hosts: String*)``:

.. includecode:: code/HttpProtocol.scala#noProxyFor
