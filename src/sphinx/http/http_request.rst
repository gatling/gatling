.. _http-request:

############
HTTP Request
############

HTTP support is a dedicated DSL, whose entry point is the ``http(requestName: Expression[String])`` method.

This request name is important because it will act as a key when computing stats for the reports.
If the same name appears in multiple places in a Simulation, Gatling will consider those requests are of the same type and theirs statistics have to be aggregated.

HTTP requests have to be passed to the ``exec()`` method in order to be attached to the scenario and be executed.
::

	// embedded style
	scenario("MyScenario")
	  .exec(http("RequestName").get("url"))

	// non embedded style
	val request = http("RequestName").get("url")
	scenario("MyScenario")
	  .exec(request)

Common parameters
=================

.. _http-request-methods:

Method and URL
--------------

HTTP protocol requires 2 mandatory parameters: the method and the URL.

Gatling provides built-ins for the most common methods. Those are simply the method name in minor case:

* ``get(url: Expression[String])``
* ``put(url: Expression[String])``
* ``post(url: Expression[String])``
* ``delete(url: Expression[String])``
* ``head(url: Expression[String])``
* ``patch(url: Expression[String])``
* ``options(url: Expression[String])``

.. note:: These methods are the ones used in REST webservices and RESTful applications; thus, such services can be tested with Gatling.

Gatling also support custom methods (e.g. you can use the method *PURGE* to purge Nginx cache):

* ``httpRequest(method: String, url: Expression[String])``
* ``httpRequestWithParams(method: String, url: Expression[String])`` for extending ``post``

This is how an HTTP request is declared::

	// general structure of an HTTP request
	http(requestName).method(url)

	// concrete examples
	http("Retrieve home page").get("https://github.com/excilys/gatling")
	http("Login").post("https://github.com/session")
	http("Nginx cache purge").httpRequest("PURGE", "http://myNginx.com")

.. _http-request-query-parameters:

Query Parameters
----------------

Frameworks and developers often pass additional information in the query, which is the part of the url after the *?*. A query is composed of *key=value* pairs, separated by *&*. Those are named *query parameters*.sb

For example, *https://github.com/excilys/gatling/issues?milestone=1&state=open* contains 2 query parameters:

* *milestone=1* : the key is *milestone* and its value is *1*
* *state=open* : the key is *state* and its value is *open*

.. note:: Query parameter keys and values have to be `URL encoded <http://www.w3schools.com/tags/ref_urlencode.asp>`_, as per `RFC3986 <http://tools.ietf.org/html/rfc3986>`_.
          Sometimes, HTTP server implementations are very permissive, but Gatling currently isn't and sticks to the RFC.

In order to set the query parameters of an HTTP request, you can:

* either pass the full query in the url, e.g.::

	http("Getting issues")
	  .get("https://github.com/excilys/gatling/issues?milestone=1&state=open")


* or pass query parameters one by one to the method named ``queryParam(key: Expression[String], value: Expression[Any])``, e.g.::

	http("Getting issues")
	  .get("https://github.com/excilys/gatling/issues")
	  .queryParam("milestone", "1")
	  .queryParam("state", "open")

Of course, you can use :ref:`Gatling Expression Language (EL) <el>` to make those values dynamic based on data in the virtual user's session::

	http("Value from session example")
	  .get("https://github.com/excilys/gatling")
	  // Global use case
	  .queryParam("myKey", "${sessionKey}")
	  // If the query parameter key and the session are the same
	  .queryParam("myKey") // Equivalent to queryParam("myKey", "${myKey}")

If you'd like to specify a query parameter without value, you have to use ``queryParam("key", "")``::

	// GET https://github.com/excilys/gatling?myKey
	http("Empty value example").get("https://github.com/excilys/gatling").queryParam("myKey", "")

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]])``::

	multivaluedQueryParam("multi", "${foo}")) // where foo is the name of a Seq Session attribute
	multivaluedQueryParam("multi", Seq("foo", "bar")))
	multivaluedQueryParam("multi", session => Seq("foo", "bar")))

If you want to add multiple query parameters at once, there are two suitable methods:

* ``queryParamsSeq(seq: Expression[Seq[(String, Any)]])``

::

  http("Getting issues")
    .get("https://github.com/excilys/gatling/issues")
    .queryParamsSeq(Seq(("milestone", "1"), ("state", "open")))

* ``queryParamsMap(map: Expression[Map[String, Any]])``

::

  http("Getting issues")
    .get("https://github.com/excilys/gatling/issues")
    .queryParamsMap(Map("milestone" -> "1", "state" -> "open"))

.. note:: As all method parameters are ``Expression[T]``, i.e. 'key' parameter is an ``Expression[String]`` and so on, if you have more specific needs you can also provide an arbitrary ``Expression[T]``, i.e. a ``Session => Validation[T]`` function.
          This function will be evaluated against the user session every time this one pass through it.
          For a deeper look at `Expression` see dedicated section :ref:`here <expression>`.

.. _http-request-headers:

HTTP Headers
------------

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


.. note:: Headers keys are defined as constants usable in the scenario, for example: ``CONTENT_TYPE``.
          You can find a list of the predefined constants `here <http://gatling-tool.org/api/gatling-http/#com.excilys.ebi.gatling.http.Predef$>`_.

.. note:: There are two handful methods to help you deal with JSON requests and XML requests: ``asJSON`` and ``asXML``.
          They are equivalent to ``header(CONTENT_TYPE, APPLICATION_JSON).header(ACCEPT, APPLICATION_JSON)`` and ``header(CONTENT_TYPE, APPLICATION_XML).header(ACCEPT, APPLICATION_XML)`` respectively.

.. note:: Headers can also be defined on the ``HttpProtocol``.

.. _http-request-authentication:

Authentication
--------------

HTTP provides two authentication methods:

* BASIC
* DIGEST

To add authentication headers to a request, use the method ``basicAuth(username: Expression[String], password: Expression[String])`` or ``digestAuth(username: Expression[String], password: Expression[String])`` as follows::

	http("My BASIC secured request").get("http://my.secured.uri").basicAuth("myUser", "myPassword")

	http("My DIGEST secured request").get("http://my.secured.uri").digestAuth("myUser", "myPassword")

Gatling provide also a more generic method to add authentication: ``authRealm(realm: Expression[Realm])``.
Then the user is in charge of building a complete ``Realm`` instance suiting its needs.
The two previous methods are in fact just shortcut for building a ``Realm`` instance.

.. note:: Authentication can also be defined on the ``HttpProtocol``.

.. _http-request-outgoing-proxy:

Outgoing Proxy
--------------

You can tell Gatling to use a proxy to send the HTTP requests.
You can set the HTTP proxy, on optional HTTPS proxy and optional credentials for the proxy::

	http("Getting issues")
    .get("https://github.com/excilys/gatling/issues")
    .proxy(Proxy("myProxyHost", 8080).httpsPort(8143).credentials("myUsername","myPassword"))

.. note:: Proxy can also be defined on the ``HttpProtocol``.

.. _http-virtual-host:

Virtual Host
------------

.. _http-request-virtual-host:

You can tell Gatling to override the default computed virtual host with the method ``virtualHost(virtualHost: Expression[String])``::

  // GET https://mobile.github.com/excilys/gatling instead of GET https://www.github.com/excilys/gatling
  http("Getting issues")
    .get("https://www.github.com/excilys/gatling/issues")
    .virtualHost("mobile")

.. note:: Virtual Host can also be defined on the ``HttpProtocol``.

HTTP Checks
-----------

.. _http-request-check:

You can add checks on a request::

  http("Getting issues")
    .get("https://www.github.com/excilys/gatling/issues")
    .check(...)

See :ref:`dedicated page <http-check>`.

.. _http-request-ignore-default-checks:

For a given request, you can also disable common checks that were defined on the ``HttpProtocol`` with ``ignoreDefaultChecks``::

  http("Getting issues")
    .get("https://www.github.com/excilys/gatling/issues")
    .ignoreDefaultChecks

FollowRedirect
--------------

.. _http-request-disable-follow-rredirect:

For a given request, you can use ``disableFollowRedirect``, just like it can be done globally on the ``HttpProtocol``::

  http("Getting issues")
    .get("https://www.github.com/excilys/gatling/issues")
    .disableFollowRedirect

Logging
-------

.. _http-request-silent:

One could want to issue a request, but not log it, e.g.:

* because this request is not related to the load test, but used for initializing the system
* because this load induced is relevant, but not the metrics, for example, with static resources

One can then make the request *silent*: ::

  http("Getting issues")
    .get("https://www.github.com/excilys/gatling/issues")
    .silent

Regular HTTP request
====================

.. _http-request-body:

Request Body
------------

You can add a full body to an HTTP request with the dedicated method ``body(body)``, where body can be:

  * ``RawFileBody(path: Expression[String])`` where path is the location of a file that will be uploaded as is
  * ``ELFileBody(path: Expression[String])`` where path is the location of a file whose content will be parsed and resolved with Gatling EL engine
  * ``StringBody(string: Expression[String])``
  * ``ByteArrayBody(bytes: Expression[Array[Byte]])``
  * ``InputStreamBody(stream: Expression[InputStream])``

.. note:: When you pass a path, Gatling searches first for an absolute path in the classpath and then in the ``request-bodies`` directory.

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


Note that one can take full advantage of Scala 2.10 macros for writing template directly in Scala compiled code instead of relying on a templating engine.
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

.. note:: For simple use cases, prefer EL strings or based files, for more complex ones where programming capability is required, prefer String interpolation.

.. _http-request-body-parts:

Multipart Request
-----------------

You can add a multipart body to an HTTP request and add parts with the dedicated method ``bodyPart(bodyPart)``, where bodyPart can be:

  * ``RawFileBodyPart(name: Expression[String], path: Expression[String])`` where path is the location of a file that will be uploaded as is
  * ``ELFileBodyPart(name: Expression[String], path: Expression[String])`` where path is the location of a file whose content will be parsed and resolved with Gatling EL engine
  * ``StringBodyPart(name: Expression[String], string: Expression[String])``
  * ``ByteArrayBodyPart(name: Expression[String], bytes: Expression[Array[Byte])``

Once bootstrapped, BodyPart have the following methods for setting additional optional information:
	
* ``contentType(contentType: String)``
* ``charset(charset: String)`` if not set, will use the default one (from ``gatling.conf`` file)
* ``fileName(fileName: Expression[String])``
* ``contentId(contentId: Expression[String])``
* ``transferEncoding(transferEncoding: Expression[String])``

.. _http-processors:

Response and request processors
_______________________________

Some people might want to process manually response or request body, Gatling request provide two hooks for that need:

* ``transformResponse(responseTransformer: ResponseTransformer)``: takes a ``RequestBody => RequestBody`` function and let one process the request body before it's being sent to the wire.
  Gatling ships two built-ins: ``gzipRequestBody`` and ``streamRequestBody``.

* ``processRequestBody(processor: Body => Body)``: takes a ``Response => Response`` function and let one process the response before it's being sent to the checks pipeline.

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
=================

.. _http-post-parameters:

POST Parameters
---------------

POST requests can have parameters defined in their body.
This is typically used for form submission, where all the values are stored as POST parameters in the body of the request.

To add such parameters to a POST request, you must use the method ``param(key: Expression[String], value: Expression[Any])`` which is actually the same as ``queryParam`` in **terms of usage** (it has the same signatures).

::

	http("My Form Data").post("my.form-action.uri")
	  .param("myKey", "myValue")

As for ``queryParam`` you have two methods to add multiple parameters at once:

* ``paramsSeq(seq: Expression[Seq[(String, Any)]])``

::

  http("My Form Data").post("my.form-action.uri")``
    .paramsSeq(Seq(("myKey", "myValue"), ("anotherKey", "anotherValue")))

* ``paramsMap(map: Expression[Map[String, Any]])``

::

  http("My Form Data").post("my.form-action.uri")
    .paramsMap(Map("myKey" -> "myValue", "anotherKey" -> "anotherValue"))

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multivaluedParam(key: Expression[String], values: Expression[Seq[Any]])``::

	multiValuedParam("omg", "${foo}")) // where foo is the name of a Seq Session attribute
	multiValuedParam("omg", List("foo", "bar")))
	multiValuedParam("omg", session => List("foo", "bar")))

The method ``param`` can also take directly an `HttpParam` instance, if you want to build it by hand.

.. _http-multipart-form:

Multipart Form
--------------

This applies only for POST requests. When you find forms asking for text values and a file to upload (usually an email attachment), your browser will send a multipart encoded request.

To define such a request, you have to add the parameters as stated above, and the file to be uploaded at the same time with the following method: ``formUpload(name: Expression[String], filePath: Expression[String])``, *name* and *filePath* can be *String*, *EL* or *Expression[String]*.

The uploaded file must be located in *user-files/request-bodies*. The *Content-Type* header will be set to *multipart/form-data* and the file added in addition to the parameters.

One can call ``formUpload()`` multiple times in order to upload multiple files.
::

	http("My Multipart Request").post("my.form-action.uri")
	  .param("myKey", "myValue")
	  .formUpload("myKey2", "myAttachment.txt")

.. note:: The MIME Type of the uploaded file defaults to ``application/octet-stream`` and the character set defaults to the one configured in ``gatling.conf`` (``UTF-8`` by default).
          Don't forget to override them when needed.

.. note:: There are is a handful method to help you deal with multipart form requests: ``asMultipartForm``.
          It is equivalent to ``header(CONTENT_TYPE, MULTIPART_FORM_DATA).
          If you use ``formUpload`` the header is automatically set for you.