.. _http-protocol:

#############
HTTP Protocol
#############

HTTP is the main protocol Gatling targets, so that's where we place most of our efforts.

Gatling HTTP allows you to load test web applications, web services or websites.
It supports HTTP and HTTPS with almost every existing features of common browsers such as caching, cookies, redirect, etc.

However, Gatling **is not a browser**: it won't run javascript, won't apply css styles and trigger css background-images download, won't react to UI events, etc.
Gatling works on the HTTP protocol level.

Bootstrapping
=============

Use the ``http`` object in order to create an HTTP protocol.

As every protocol in Gatling, the HTTP protocol can be configured for a scenario.
This is done thanks to the following statements::

	val httpConf = http.baseURL("http://my.website.tld")
	...
	setUp(scn.protocols(httpConf))

Core parameters
===============

.. _http-protocol-base-url:

Base URL
--------

As you may have seen in the previous example, you can set a base URL.
This base URL will be prepended to all urls that does not start with ``http``, eg::

	val httpConf = http.baseURL("http://my.website.tld")

	val scn = scenario("My Scenario")
	  .exec(
	    http("My Request")
	    .get("/my_path") // Will actually make a request on "http://my.website.tld/my_path"
	  )
	  .exec(
	    http("My Other Request")
	    .get("http://other.website.tld") // Will make a request on "http://other.website.tld"
	  ...

	setUp(scn.protocolConfig(httpConf)...)

Load testing several servers with client based load balancing
-------------------------------------------------------------

If you want to load test several servers at the same time, to bypass a load-balancer for example, you can use methods named ``baseURLs`` which accepts a ``String*`` or a ``List[String]``::

	val httpConf = http.baseURLs("http://my1.website.tld", "http://my2.website.tld", "http://my3.website.tld")

The selection of the URL is made at each request, using the ``Random`` generator.


.. _http-protocol-warmup:

Automatic warm up
-----------------

The Java/NIO engine start up introduces an overhead on the first request to be executed.
In order to compensate this effect, Gatling automatically performs a request to http://gatling-tool.org.

To disable this feature, just add ``.disableWarmUp`` to an HTTP Protocol Configuration definition.
To change the warm up url, just add ``.warmUp("newUrl")``.

::

    // override warm up URL to www.google.com
    val httpConf = http.warmUp("www.google.com")
    // disable warm up
    val httpConfNoWarmUp = http.disableWarmUp

Engine parameters
=================

.. _http-protocol-max-connection:

Max connection per host
-----------------------

In order to mimic real web browser, you can configure the max concurrent connections per host **per virtual user**  with ``maxConnectionsPerHost(max: Int)``.
Gatling ships a bunch of built-ins for well-known browser:

* maxConnectionsPerHostLikeFirefoxOld
* maxConnectionsPerHostLikeFirefox
* maxConnectionsPerHostLikeOperaOld
* maxConnectionsPerHostLikeOpera
* maxConnectionsPerHostLikeSafariOld
* maxConnectionsPerHostLikeSafari
* maxConnectionsPerHostLikeIE7
* maxConnectionsPerHostLikeIE8
* maxConnectionsPerHostLikeIE10
* maxConnectionsPerHostLikeChrome

::

    // 10 connections per host.
    val httpConf= http.maxConnectionsPerHost(10)
    // Firefox max connections per host preset.
    val httpConf= http.maxConnectionsPerHostLikeFirefox

.. _http-protocol-connection-sharing:

Connections sharing
-------------------

In Gatling 1, connections are shared amongst users until 1.5 version.
This behavior does not match real browsers, and doesn't support SSL session tracking.

In Gatling 2, the default behavior is that every user has his own connection pool.
This can be tuned with the ``shareConnections`` configuration param.

If you need more isolation of your user, for instance if you need a dedicated key store per user,
Gatling lets you have an instance of the http client per user with ``disableClientSharing``.

Virtual Host
------------

.. _http-protocol-virtual-host:

On can set a different Host than the url one::

  virtualHost(virtualHost: Expression[String])

Local address
-------------

.. _http-protocol-local-address:

One can bind teh sockets from a specific local address instead of the default one::

  localAddress(localAddress: InetAddress)


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

Gatling supports this caching feature:

* Expires header
* Cache-Control header
* Last-Modified header
* ETag

To disable this feature, just add ``.disableCaching`` to an HTTP Protocol Configuration definition.

.. _http-protocol-headers:

HTTP Headers
------------

Gatling lets you set some generic headers at the http protocol definition level with ``baseHeaders(headers: Map[String, String])``.
You have also the following built-ins for the more commons headers:

* acceptHeader(value: Expression[String]): set ``Accept`` header.
* acceptCharsetHeader(value: Expression[String]): set ``Accept-Charset`` header.
* acceptEncodingHeader(value: Expression[String]): set ``Accept-Encoding`` header.
* acceptLanguageHeader(value: Expression[String]): set ``Accept-Language`` header.
* authorizationHeader(value: Expression[String]): set ``Authorization`` header.
* doNotTrackHeader(value: Expression[String]): set ``DNT`` header.
* userAgentHeader(value: Expression[String]): set ``User-Agent`` header.

.. _http-protocol-auth:

Authentication
--------------

You can set the authentication methods at protocol level with these methods:

* basicAuth(username: Expression[String], password: Expression[String])
* digestAuth(username: Expression[String], password: Expression[String])
* authRealm(realm: Expression[Realm])

.. note:: For more details see the dedicated section :ref:`here <http-request-authentication>`.

Response handling parameters
============================

.. _http-protocol-redirect:

Follow redirects
----------------

By default Gatling automatically follow redirects in case of 301 or 302 response status code, you can disable this behaviour with ``disableFollowRedirect``.

To avoid infinite redirection loops, you can specify a number max of redirects with:  ``maxRedirects(max: Int)``

.. _http-protocol-chunksdiscard:

Response chunks discarding
--------------------------

Beware that, as an optimization, Gatling doesn't pile up response chunks unless a check is defined on the response body.
However some people might want always keep the response chunks, thus you can disable the default behaviour with ``disableResponseChunksDiscarding``.

.. _http-protocol-extractor:

Dumping custom data
-------------------

Some people might want more data than what Gatling normally dumps in the ``simulation.log`` file.

Http protocol provide a hook for dumping extra data with ``extraInfoExtractor(f: ExtraInfoExtractor)``.
``ExtraInfoExtractor`` is a shortcut for the function type: ``(String, Status, Session, Request, Response) => List[Any]``.
Thus your extractor need to return a ``List[Any]``, ``Any`` is the equivalent of ``Object`` in Scala, and have access to:

* The name of the request.
* The status of the request, i.e. OK/KO.
* The user Sesion.
* The http request.
* The http response.

The extra data will be appended to the relative records in the ``simulation.log`` file and reports generation will ignore them.
It's up to the user to build his own analysis system for them.

.. _http-protocol-processor:

Response and request processors
-------------------------------

Some people might want to process manually response, Gatling protocol provide a hook for that need: ``transformResponse(responseTransformer: ResponseTransformer)``

.. note:: For more details see the dedicated section :ref:`here <http-processors>`.

.. _http-protocol-check:

Checks
------

You can define checks at the http protocol definition level with: ``check(checks: HttpCheck*)``.
They will be apply on all the requests, however you can disable them for given request thanks to thanks to the ``ignoreDefaultChecks`` method.

.. note:: For more details see the dedicated section :ref:`here <http-check>`.

.. _http-protocol-fetch:

Resource fetching
-----------------

Gatling allow to fetch resources in parallel in order to emulate the behaviour of a real web browser.
At the request level you can use the ``resources(res: AbstractHttpRequestBuilder[_]*)`` to fetch specific resources.

Or you can use ``fetchHtmlResources`` methods at the protocol definition level.
Thus Gatling will automatically parse HTML to find embedded resources in the dom and load them asynchronously.
The supported resources are:

* <script>
* <base>
* <link>
* <bgsound>
* <frame>
* <iframe>
* <img>
* <input>
* <body>
* <applet>
* <embed>
* <object>
* import directives in HTML
* @import CSS rule

You can also specify black/whith list or custom filters to have a more fine grain control on resource fetching.
``WhiteList`` and ``BlackList`` take a sequence of pattern, eg ``Seq("www.google.com/.*", "www.github.com/.*")``, to include and exclude respectively.

* ``fetchHtmlResources(white: WhiteList)``: fetch all resources matching a pattern in the white list.
* ``fetchHtmlResources(white: WhiteList, black: BlackList)``: fetch all resources matching a pattern in the white list excepting those in the black list.
* ``fetchHtmlResources(black: BlackList, white: WhiteList = WhiteList(Nil))``: fetch all resources excepting those matching a pattern in the black list and not in the white list.
* ``fetchHtmlResources(filters: Option[Filters])``

.. _http-protocol-proxy:

Proxy parameters
----------------

You can tell Gatling to use a proxy to send the HTTP requests.
You can set the HTTP proxy, on optional HTTPS proxy and optional credentials for the proxy::

	val httpConf = http.proxy(Proxy("myProxyHost", 8080).httpsPort(8143).credentials("myUsername","myPassword"))

You can also disabled the use of proxy for a given list of host with ``noProxyFor(hosts: String*)``::

    val httpConf = http.proxy(Proxy("myProxyHost", 8080)).noProxyFor("www.github.com", "www.akka.io")
