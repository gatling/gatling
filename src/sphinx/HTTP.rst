.. _http:

****
HTTP
****

As the first protocol module of Gatling, Gatling HTTP allows you to load test web applications, web services or websites.
It supports HTTP and HTTPS with almost every existing features of common browsers.

HTTP requests are declared in ``exec()`` method, eg::

	exec( http(...).queryParam(...) )

Gatling HTTP protocol is a dedicated DSL


Building a request
==================

Common parameters
-----------------

.. _http-methods:

Methods and URIs
^^^^^^^^^^^^^^^^

If you know HTTP protocol, you certainly know that for a request to be sent, there are mandatory parameters to be set. The first ones are the Method and the URI of the request.

Gatling currently supports all methods of the HTTP protocol and we have built-ins for the followings:

* OPTIONS - Used to get communication **options** on the URI
* HEAD - Used to **head** information stored at the URI
* GET - Used to **get** information stored at the URI
* POST - Used to **post** information to an HTTP server
* PUT - Used to **update** existing information at the URI
* DELETE - Used to **delete** existing information at the URI

.. note:: These methods are the ones used in REST webservices and RESTful applications; thus, such services can be tested with Gatling.

.. note:: Gatling also support custom methods, eg you can use the methods ``PURGE`` to purge Nginx cache.

This is how an HTTP request is declared::

	// general structure of an HTTP request
	http("Name of the request").method(URI)

	// concrete examples
	http("Retrieve home page").get("https://github.com/excilys/gatling")
	http("Login").post("https://github.com/session")
	http("Nginx cache purge").httpRequest("PURGE", "http://myNginx.com")

.. _http-query-parameters:

Query Parameters
^^^^^^^^^^^^^^^^

To send information to a web server, frameworks and developers use query parameters, you can find them after the ``?`` of an URI::

    https://github.com/excilys/gatling/issues?milestone=1&state=open

Here the query parameters are:

* *milestone=1* : the key is ``milestone`` and its value is ``1``
* *state=open* : the key is ``state`` and its value is ``open``

To define the query parameters of an HTTP request, you can use the method named ``queryParam(key: Expression[String], value: Expression[Any])``, eg::

	// GET https://github.com/excilys/gatling/issues?milestone=1&state=open
	http("Getting issues")
	  .get("https://github.com/excilys/gatling/issues")
	  .queryParam("milestone", "1")
	  .queryParam("state", "open")

You can use ELs (defined :ref:`here <the-session>`) to get values from the session::

	// GET https://github.com/excilys/gatling?myKey={valueFromSession}
	http("Value from session example").get("https://github.com/excilys/gatling")
	  // Global use case
	  .queryParam("myKey", "${sessionKey}")
	  // If the query parameter key and the session are the same
	  .queryParam("myKey") // Equivalent to queryParam("myKey", "${myKey}")

If you'd like to specify a query parameter without value, you must use ``queryParam("key", "")``::

	// GET https://github.com/excilys/gatling?myKey
	http("Empty value example").get("https://github.com/excilys/gatling").queryParam("myKey", "")

The method ``queryParam`` can also take directly an ``HttpParam`` instance, if you want to build it by hand.

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]])``::

	multiValuedQueryParam("omg", "foo")) // where foo is the name of a Seq Session attribute
	multiValuedQueryParam("omg", List("foo")))
	multiValuedQueryParam("omg", session => List("foo")))

If you want to add multiple query params at once, there are two suitable methods:

* ``queryParamsSequence(seq: Expression[Seq[(String, Any)]])``::

    http("Getting issues")
      .get("https://github.com/excilys/gatling/issues")
      .queryParamsSequence(Seq(("milestone", "1"), ("state", "open")))

* ``queryParamsMap(map: Expression[Map[String, Any]])``::

    http("Getting issues")
      .get("https://github.com/excilys/gatling/issues")
      .queryParamsMap(Map("milestone" -> "1", "state" -> "open"))

.. note:: As all method parameters are ``Expression[T]``, i.e. 'key' parameter is an ``Expression[String]`` and so on,
          if you have more specific needs you can also provide an arbitrary ``Expression[T]``, i.e. a ``Session => Validation[T]`` function.
          This function will be evaluated against the user session every time this one pass through it.
          For a deeper look at `Expression` see dedicated section :ref:`here <expression>`.

.. _http-headers:

HTTP Headers
^^^^^^^^^^^^

HTTP protocol uses headers to exchange information between client and server that is not part of the message (stored in the body of the request, if there is one).
Gatling HTTP allows you to specify any header you want to with the ``header(name: String, value: Expression[String])`` and ``headers(newHeaders: Map[String, String])`` methods.
Here are some examples::

  // Defining a map of headers before the scenario allows you to reuse these in several requests
    val sentHeaders = Map("Content-Type" -> "application/javascript", "Accept" -> "text/html")

	scenario(...)
	  ...
	    http(...).post(...)
	      // Adds several headers at once
	      .headers(sentHeaders)
	      // Adds another header to the request
	      .header("Keep-Alive", "150")
	      // Overrides the Content-Type header
	      .header("Content-Type", "application/json")


.. note:: headers keys are defined as constants usable in the scenario, for example: ``CONTENT_TYPE``.
          You can find a list of the predefined constants `here <http://gatling-tool.org/api/gatling-http/#com.excilys.ebi.gatling.http.Predef$>`_.

.. note:: There are two handful methods to help you deal with JSON requests and XML requests: ``asJSON`` and ``asXML``.
          They are equivalent to ``header(CONTENT_TYPE, APPLICATION_JSON).header(ACCEPT, APPLICATION_JSON)`` and ``header(CONTENT_TYPE, APPLICATION_XML).header(ACCEPT, APPLICATION_XML)`` respectively.

.. _http-authentication:

Authentication
^^^^^^^^^^^^^^

HTTP provides two authentication methods to secure URIs:

* BASIC
* DIGEST

Gatling supports both authentication.

To add authentication headers to a request, you must use the method ``basicAuth(username: String, password: String)`` or ``digestAuth(username: Expression[String], password: Expression[String])`` as follows::

	http("My BASIC secured request").get("http://my.secured.uri").basicAuth("myUser", "myPassword")

	http("My DIGEST secured request").get("http://my.secured.uri").digestAuth("myUser", "myPassword")

Gatling provide also a more generic method to add authentication: ``authRealm(realm: Expression[Realm])``.
Then the user is in charge of building a complete ``Realm`` instance suiting its needs.
The two previous methods are in fact just shortcut for building a ``Realm`` instance.

.. _http-outgoing-proxy:

Outgoing Proxy
^^^^^^^^^^^^^^

You can tell Gatling to use a proxy to send the HTTP requests.
You can set the HTTP proxy, on optional HTTPS proxy and optional credentials for the proxy::

	http("Getting issues")
      .get("https://github.com/excilys/gatling/issues")
      .proxy(Proxy("myProxyHost", 8080).httpsPort(8143).credentials("myUsername","myPassword"))

.. _http-virtual-host:

Virtual Host
^^^^^^^^^^^^

You can tell Gatling to override the default computed virtual host with the method ``virtualHost(virtualHost: Expression[String])``::

  // GET https://mobile.github.com/excilys/gatling instead of GET https://www.github.com/excilys/gatling
  http("Getting issues")
      .get("https://www.github.com/excilys/gatling/issues")
      .virtualHost("mobile")


Regular HTTP request
--------------------

.. _http-request-body:

Request Body
^^^^^^^^^^^^

You can add a body to an http request with to dedicated methods:

* ``body(body)`` where body can be:

  * ``ELFileBody(path)`` where path is the location of a EL template file, can be a String or an Expression[String]
  * ``StringBody(string)`` where string can be a String or an Expression[String]
  * ``RawFileBody(path)`` where path is the location of a file, can be String or an Expression[String]
  * ``ByteArrayBody(bytes)`` where bytes can be an Array[Byte] or an Expression[Array[Byte]]
  * ``InputStreamBody(stream)`` where stream can be an InputStream or an Expression[InputStream]

* ``bodyPart(bodyPart)`` for multipart request, where bodyPart can be:

  * ``RawFileBodyPart(name, filePath, contentType)``
  * ``ELFileBodyPart(name, filePath)``
  * ``StringBodyPart(name, string, charset, contentType, transferEncoding, contentId)``
  * ``ByteArrayBodyPart(name, bytes, charset, contentType, transferEncoding, contentId)``
  * ``FileBodyPart(name, file, charset, contentType, transferEncoding, contentId)``

Eg::

    http("String body").post("my.post.uri")
      .body(StringBody("""{ "myContent": "myValue" }""")).asJSON

::

	/* user-files/request-bodies/myFileBody.json */
	{ "myContent": "${myValue}" }

::

	/* Scenario */
	http("Template Body").post("my.post.uri")
	  .body(ELFileBody("myFileBody.json")).asJSON

.. note:: When you pass a path, Gatling searches firstly for an absolute path in the classpath and then in user-files/request-bodies directory.

Note that one can take full advantage of Scala 2.10 macros for writing template directly in Scala compiled code instead of relying on a template engine.
See `Scala 2.10 string interpolation <(http://docs.scala-lang.org/overviews/core/string-interpolation.html>`_ and `Fastring <https://github.com/Atry/fastring>`_.

For example::

	object Templates {
	  val template: Expression[String] = (session: Session) =>
	  for {
	    foo = session("foo").validate[String]
	    bar = session("bar").validate[String]
	  } yield s"""{
	    foo: $foo,
	    bar: $bar
	  }"""
	}

.. note:: For simple use cases, prefer EL based files, for more complex ones where programming capability is required, prefer standard String interpolation.

.. _http-max-redirects:

Max redirects
^^^^^^^^^^^^^

By default Gatling automatically follow redirects in case of 301 or 302 response status code.
To avoid infinite redirection loops, you can specify a number max of redirects with:  ``maxRedirects(max: Int)``

.. _http-dumping-custom-data:

Dumping custom data
^^^^^^^^^^^^^^^^^^^

Some people might want more data than what Gatling normally dumps in the ``simulation.log`` file.

Http requests provide a hook for dumping extra data with ``extraInfoExtractor(f: ExtraInfoExtractor)``.
``ExtraInfoExtractor`` is a shortcut for the function type: ``(String, Status, Session, Request, Response) => List[Any]``.
Thus your extractor need to return a ``List[Any]``, ``Any`` is the equivalent of ``Object`` in Scala, and have access to:

* The name of the request.
* The status of the request, i.e. OK/KO.
* The user Sesion.
* The http request.
* The http response.

The extra data will be appended to the relative records in the ``simulation.log`` file and reports generation will ignore them.
It's up to the user to build his own analysis system for them.

.. _http-processors:

Response and request processors
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Some people might want to process manually response or request body, Gatling request provide two hooks for that need:

* ``transformResponse(responseTransformer: ResponseTransformer)``: takes a ``RequestBody => RequestBody`` function and let one process the request body before it's being sent to the wire.
  Gatling ships two built-ins: ``gzipRequestBody`` and ``streamRequestBody``.

* ``processRequestBody(processor: Body => Body)``: takes a ``Response => Response`` function and let one process the response before it's being sent to the checks pipeline.

.. _http-checks:

Checks
^^^^^^

Concepts
~~~~~~~~

The Check API is used for verifying that the response to a request matches expectations and capturing some elements in it.

Checks are performed on a request thanks to the method ``check``.
For example, on an HTTP request ::

	http("My Request").get("myUrl").check(status.is(200))

One can of course perform multiple checks::

	http("My Request").get("myUrl").check(status.not(404), status.not(500)))


This API provides a dedicated DSL for chaining the following steps:

1. defining the check
2. extracting
3. transforming
4. verifying
5. saving

Defining the check type
~~~~~~~~~~~~~~~~~~~~~~~

The HTTP Check implementation provides the following built-ins:

HTTP status
************

.. _checks-status:

* ``status``

Targets the HTTP response status code.

.. note:: A status check is automatically added to a request when you don't specify one.
          It checks that the HTTP response has a 2XX status code.

.. _checks-current-location:

* ``currentLocation``

Targets the current page absolute URL.
Useful when following redirects in order to check if the landing page is indeed the expected one.


HTTP header
************

.. _checks-header:

* ``header(headerName)``

Targets the HTTP response header of the given name.
*headerName* can be a simple String, an evaluable String containing expression, or an Expression[String].

.. _check-headerRegex:

* ``headerRegex(headerName, pattern)``

Same than above, but *pattern* is used to apply a regex on the header value.

.. note:: The header names are available as constants in the DSL. They all are written in upper case and words are separated with underscores, eg: CONTENT_TYPE

.. note:: ``Location`` header value is automatically decoded when performing a check on it

.. _checks-response-body:

HTTP response body:
*******************

HTTP checks are performed in the order of HTTP element precedence: first status, then headers, then response body.

Beware that, as an optimization, Gatling doesn't pile up response chunks unless a check is defined on the response body.

.. _check-response-time:

* ``responseTimeInMillis``

Returns the response time of this request in milliseconds = the time between starting to send the request and finishing to receive the response.

* ``latencyInMillis``

Returns the latency of this request in milliseconds = the time between finishing to send the request and starting to receive the response.

* ``bodyString``

Return the full response body.

* ``regex(expression)``

Defines a Java regular expression to be applied on any text response body.

*expression* can be a simple String, an evaluatable String containing expression, or an Expression[String].

It can contain 0 or 1 capture group.

::

	regex("""<td class="number">""")
	regex("""<td class="number">ACC${account_id}</td>""")
	regex("""/private/bank/account/(ACC[0-9]*)/operations.html""")

.. note:: In Scala, you can use escaped strings with this notation: ``"""my "non-escaped" string"""``.
          This simplifies the writing and reading of regular expressions.

* ``xpath(expression, namespaces)``

Defines an XPath 1.0 expression to be applied on an XML response body.

*expression* can be a simple String, an evaluatable String containing expression, or an Expression[String].

*namespaces* is an optional List of couples of (prefix, uri)

::

	xpath("//input[@id='text1']/@value")
	xpath("//foo:input[@id='text1']/@value", List("foo" -> "http://foo.com"))

.. note:: You can also use vtdXpath(xpathExpression: String), this check uses VTD as the XPath engine,
          it is available as a `separate module <https://github.com/excilys/gatling-vtd>`_.

* ``jsonPath(expression)``

Based on `Goessner's JsonPath <http://goessner.net/articles/JsonPath>`_.

*expression* can be a simple String, a String containing an EL expression, or an Expression[String].

::

	jsonPath("$..foo.bar[2].baz")

.. note:: In JSON, the root element has no name.
          This might be a problem when it's an array and one want to target its elements.
          As a workaround, Gatling names it ``_``.

.. _checks-css:

* ``css(expression, attribute)``

Gatling supports `CSS Selectors <http://jodd.org/doc/csselly>`_.

*expression* can be a simple String, a String containing an EL expression, or a (Session => String) function.

*attribute* is an optional String.
When filled, check is performed against the attribute value.
Otherwise check is performed against the node text content.

.. _checks-checksum:

* ``md5`` and ``sha1``

Returns a checksum of the response body.
Checksums are computed efficiently against body parts as soon as there's received.
Those are then discarded if not needed.

.. note:: checksums are computed against the stream of chunks, so the whole body is not stored in memory.

Extracting
~~~~~~~~~~

* ``find``: return the first occurrence

* ``find(occurrence)``: return the occurrence of the given rank

.. note:: Ranks start at 0.

* ``findAll``: return a List of all the occurrences

* ``count``: return the number of occurrences

find(occurrence), findAll and count are only available on check types that might produce multiple results.
For example, status only has find.

.. note:: In case of no extracting step is defined, a ``find`` is added implicitly.

Transforming
~~~~~~~~~~~~

``transform(transformationFunction)``

Transforming is an **optional** step for transforming the result of the extraction before trying to match or save it.

*transformationFunction* is a function whose input is the extraction result and output is the result of your transformation.

::

	transform(string => string + "foo")

Verifying
~~~~~~~~~

* ``is(expected)``

Checks that the value is equal to the expected one.

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).
In case of a String, it can also be a static String or a String with an EL expression.

* ``not(expected)``

Checks that the value is different from the expected one.

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).
In case of a String, it can also be a static String or a String with an EL expression.

* ``exists``

Checks that the value exists and is not empty in case of multiple results.

* ``notExists``

Checks that the value doesn't exist and or is empty in case of multiple results.

* ``in(sequence)``

Checks that the value belongs to a given sequence.

.. _checks-whatever:

* ``dontValidte``

Always true, used for capture an optional value.

*expected* is a function that returns a sequence of values of the same type of the previous step (extraction or transformation).

.. note:: In case of no verifying step is defined, a `exists`` is added implicitly.

Saving
~~~~~~

``saveAs(key)``

Saving is an optional step for storing the result of the previous step (extraction or transformation) into the virtual user Session, so that it can be reused later.

*key* is a String

Putting it all together
~~~~~~~~~~~~~~~~~~~~~~~

To help you understand the checks, here is a list of examples:

::

	check(regex("""https://(.*)""").count.is(5))

Verifies that there are exactly 5 HTTPS links in the response

::

	check(regex("""https://(.*)/.*""")
	      .findAll
	      .is(List("www.google.com", "www.mysecuredsite.com"))

Verifies that there are two secured links pointing at the specified websites.

::

	check(status.is(200))

Verifies that the status is equal to 200

::

	check(status.in(200 to 210))

Verifies that the status is one of: 200, 201, 202, ..., 209, 210

::

	check(regex("aWord").find(1).exists))

Verifies that there are at least **two** occurrences of "aWord"

::

	check(regex("aWord").notExists)

Verifies that the response doesn't contain "aWord"

.. _http-resource-fetching:

Resources fetching
^^^^^^^^^^^^^^^^^^

Gatling allow to fetch resources in parallel in order to emulate the behaviour of a real web browser.
To do that you can use ``fetchHtmlResources`` methods at the protocol definition level.
Or at the request level you can use the ``resources(res: AbstractHttpRequestBuilder[_]*)``.

For example::

  http("Getting issues")
      .get("https://www.github.com/excilys/gatling/issues")
      .resources(
          http("api.js").get("https://collector-cdn.github.com/assets/api.js"),
          http("ga.js").get("https://ssl.google-analytics.com/ga.js"))


POST HTTP request
-----------------

.. _http-post-parameters:

POST Parameters
^^^^^^^^^^^^^^^

OST requests can have parameters defined in their body.
This is typically used for form submission, where all the values are stored as POST parameters in the body of the request.

To add such parameters to a POST request, you must use the method ``param(key: Expression[String], value: Expression[Any])`` which is actually the same as ``queryParam`` in **terms of usage** (it has the same signatures).

::

	http("My Form Data").post("my.form-action.uri")
	  .param("myKey", "myValue")

As for ``queryParam`` you have two methods to add multiple parameters at once:

* paramsSequence(seq: Expression[Seq[(String, Any)]])::

    http("My Form Data").post("my.form-action.uri")
      .paramsSequence(Seq(("myKey", "myValue"), ("anotherKey", "anotherValue")))

* paramsMap(map: Expression[Map[String, Any]])::

    http("My Form Data").post("my.form-action.uri")
      .paramsMap(Map("myKey" -> "myValue", "anotherKey" -> "anotherValue"))

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multivaluedParam(key: Expression[String], values: Expression[Seq[Any]])``::

	multiValuedParam("omg", "${foo}")) // where foo is the name of a Seq Session attribute
	multiValuedParam("omg", List("foo", "bar")))
	multiValuedParam("omg", session => List("foo", "bar")))

The method ``param`` can also take directly an `HttpParam` instance, if you want to build it by hand.

.. _http-multipart-request:

Multipart encoded requests
^^^^^^^^^^^^^^^^^^^^^^^^^^

This applies only for POST requests. When you find forms asking for text values and a file to upload (usually an email attachment), your browser will send a multipart encoded request.

To define such a request, you have to add the parameters as stated above, and the file to be uploaded at the same time with the following method::

	formUpload(name: Expression[String], filePath: Expression[String])

``name`` and ``filePath`` can be String, EL or Expression[String].

The uploaded file must be located in ``user-files/request-bodies``. The Content-Type header will be set to "multipart/form-data" and the file added in addition to the parameters.

One can call ``formUpload`` multiple times in order to upload multiple files.
::

	http("My Multipart Request").post("my.form-action.uri")
	  .param("myKey", "myValue")
	  .formUpload("myKey2", "myAttachment.txt")

.. note:: The MIME Type of the uploaded file defaults to ``application/octet-stream`` and the character set defaults to the one configured in ``gatling.conf`` (``UTF-8`` by default).
          Don't forget to override them when needed.

.. note:: There are is a handful method to help you deal with multipart form requests: ``asMultipartForm``.
          It is equivalent to ``header(CONTENT_TYPE, MULTIPART_FORM_DATA).
          If you use ``formUpload`` the header is automatically set for you.

WebSockets
----------










Configuring HTTP Protocol
=========================

As every protocol in Gatling, the HTTP protocol can be configured for a scenario. This is done thanks to the following statements::

	val httpConf = http.baseURL("http://my.website.tld")
	...
	setUp(scn.protocols(httpConf))

Core parameters
---------------

.. _http-base-url:

Base URL
^^^^^^^^

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

Stressing several servers with client based load balancing
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you want to stress several servers at the same time, to bypass a load-balancer for example, you can use methods named ``baseURLs`` which accepts a ``String*`` or a ``List[String]``::

	val httpConf = http.baseURLs("http://my1.website.tld", "http://my2.website.tld", "http://my3.website.tld")

The selection of the URL is made at each request, using the ``Random`` generator.


.. _http-warmup:

Automatic warm up
^^^^^^^^^^^^^^^^^

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
-----------------

.. _http-max-connection:

Max connection per host
^^^^^^^^^^^^^^^^^^^^^^^

You can configure the max parallel connections per host with ``maxConnectionsPerHost(max: Int)`` in order to mimic real web browser.
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

.. _http-connection-sharing:

Connections sharing
^^^^^^^^^^^^^^^^^^^

In Gatling 1, connections are shared amongst users until 1.5 version.
This behavior does not match real browsers, and doesn't support SSL session tracking.

In Gatling 2, the default behavior is that every user has his own connection pool.
This can be tuned with the ``shareConnections`` configuration param.

If you need more isolation of your user, for instance if you need a dedicated key store per user,
Gatling lets you have an instance of the http client per user with ``disableClientSharing``.

virtualHost(virtualHost: Expression[String])
localAddress(localAddress: InetAddress)

Request building parameters
---------------------------

Automatic Referer
^^^^^^^^^^^^^^^^^

The ``Referer`` HTTP header can be automatically computed.
This feature is enabled by default.

To disable this feature, just add ``.disableAutomaticReferer`` to an HTTP Protocol Configuration definition.

Caching
^^^^^^^

Gatling supports this caching feature:

* Expires header
* Cache-Control header
* Last-Modified header
* ETag

To disable this feature, just add ``.disableCaching`` to an HTTP Protocol Configuration definition.

HTTP Headers
^^^^^^^^^^^^

Gatling lets you set some generic headers at the http protocol definition level with ``baseHeaders(headers: Map[String, String])``.
You have also the following built-ins for the more commons headers:

* acceptHeader(value: Expression[String]): set ``Accept`` header.
* acceptCharsetHeader(value: Expression[String]): set ``Accept-Charset`` header.
* acceptEncodingHeader(value: Expression[String]): set ``Accept-Encoding`` header.
* acceptLanguageHeader(value: Expression[String]): set ``Accept-Language`` header.
* authorizationHeader(value: Expression[String]): set ``Authorization`` header.
* doNotTrackHeader(value: Expression[String]): set ``DNT`` header.
* userAgentHeader(value: Expression[String]): set ``User-Agent`` header.

connection(value: Expression[String])

Authentication
^^^^^^^^^^^^^^

You can set the authentication methods at protocol level with these methods:

* basicAuth(username: Expression[String], password: Expression[String])
* digestAuth(username: Expression[String], password: Expression[String])
* authRealm(realm: Expression[Realm])

.. note:: For more details see the dedicated section :ref:`here <http-authentication>`.

Response handling parameters
----------------------------

Follow redirects
^^^^^^^^^^^^^^^^

By default Gatling automatically follow redirects in case of 301 or 302 response status code, you can disable this behaviour with ``disableFollowRedirect``.

To avoid infinite redirection loops, you can specify a number max of redirects with:  ``maxRedirects(max: Int)``

Response chunks discarding
^^^^^^^^^^^^^^^^^^^^^^^^^^

Beware that, as an optimization, Gatling doesn't pile up response chunks unless a check is defined on the response body.
However some people might want always keep the response chunks, thus you can disable the default behaviour with ``disableResponseChunksDiscarding``.

Dumping custom data
^^^^^^^^^^^^^^^^^^^

Some people might want more data than what Gatling normally dumps in the ``simulation.log`` file.

Http protocol provide a hook for dumping extra data with ``extraInfoExtractor(f: ExtraInfoExtractor)``.

.. note:: For more details see the dedicated section :ref:`here <http-dumping-custom-data>`.

Response and request processors
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Some people might want to process manually response, Gatling protocol provide a hook for that need: ``transformResponse(responseTransformer: ResponseTransformer)``

.. note:: For more details see the dedicated section :ref:`here <http-processors>`.

Checks
^^^^^^

You can define checks at the http protocol definition level with: ``check(checks: HttpCheck*)``.
They will be apply on all the requests, however you can disable them for given request thanks to thanks to the ``ignoreDefaultChecks`` method.

.. note:: For more details see the dedicated section :ref:`here <http-checks>`.

Resource fetching
^^^^^^^^^^^^^^^^^

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


WebSockets parameters
---------------------

Base URL
^^^^^^^^

You can set a base URL for websocket, it will be prepended to all urls that does not start with ``ws``::

    ``wsBaseURL(baseUrl: String)``

If you want to stress several servers at the same time, to bypass a load-balancer for example, you can use following methods:

* ``wsBaseURLs(baseUrl1: String, baseUrl2: String, baseUrls: String*)``
* ``wsBaseURLs(baseUrls: List[String])``

Reconnection
^^^^^^^^^^^^

If a websocket is closed on the server side, Gatling can automatically open a new websocket to reconnect to the server.
This is done with the ``wsReconnect`` option.
You can also specify the maximum number of reconnects allowed for a websocket.
::

    val httpConf = http.wsReconnect.wsMaxReconnects(5)

Proxy parameters
----------------

You can tell Gatling to use a proxy to send the HTTP requests.
You can set the HTTP proxy, on optional HTTPS proxy and optional credentials for the proxy::

	val httpConf = http.proxy(Proxy("myProxyHost", 8080).httpsPort(8143).credentials("myUsername","myPassword"))

You can also disabled the use of proxy for a given list of host with ``noProxyFor(hosts: String*)``::

    val httpConf = http.proxy(Proxy("myProxyHost", 8080)).noProxyFor("www.github.com", "www.akka.io")
