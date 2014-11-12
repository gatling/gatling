.. _http-request:

############
HTTP Request
############

HTTP support has a dedicated DSL, whose entry point is the ``http(requestName: Expression[String])`` method.

This request name is important because it will act as a key when computing stats for the reports.
If the same name appears in multiple places in a Simulation, Gatling will consider those requests are of the same type and their statistics will be aggregated.

HTTP requests have to be passed to the ``exec()`` method in order to be attached to the scenario and be executed.
::

  // embedded style
  scenario("MyScenario")
    .exec(http("RequestName").get("url"))

  // non embedded style
  val request = http("RequestName").get("url")
  scenario("MyScenario")
    .exec(request)

.. _http-request-methods:

Method and URL
==============

HTTP protocol requires 2 mandatory parameters: the method and the URL.

Gatling provides built-ins for the most common methods. Those are simply the method name in minor case:

* ``get(url: Expression[String])``
* ``put(url: Expression[String])``
* ``post(url: Expression[String])``
* ``delete(url: Expression[String])``
* ``head(url: Expression[String])``
* ``patch(url: Expression[String])``
* ``options(url: Expression[String])``

.. note:: These methods are the ones used in REST web services and RESTful applications; thus, such services can be tested with Gatling.

Gatling also supports custom methods (e.g. you can use the method *PURGE* to purge Nginx cache):

* ``httpRequest(method: String, url: Expression[String])``
* ``httpRequestWithParams(method: String, url: Expression[String])`` for extending ``post``

This is how an HTTP request is declared::

  // general structure of an HTTP request
  http(requestName).method(url)

  // concrete examples
  http("Retrieve home page").get("https://github.com/gatling/gatling")
  http("Login").post("https://github.com/session")
  http("Nginx cache purge").httpRequest("PURGE", "http://myNginx.com")

.. _http-request-query-parameters:

Query Parameters
================

Frameworks and developers often pass additional information in the query, which is the part of the url after the ``?``. A query is composed of *key=value* pairs, separated by ``&``. Those are named *query parameters*.

For example, ``https://github.com/gatling/gatling/issues?milestone=1&state=open`` contains 2 query parameters:

* ``milestone=1`` : the key is *milestone* and its value is *1*
* ``state=open`` : the key is *state* and its value is *open*

.. note:: Query parameter keys and values have to be `URL encoded <http://www.w3schools.com/tags/ref_urlencode.asp>`_, as per `RFC3986 <http://tools.ietf.org/html/rfc3986>`_.
          Sometimes, HTTP server implementations are very permissive, but Gatling currently isn't and sticks to the RFC.

In order to set the query parameters of an HTTP request, you can:

* either pass the full query in the url, e.g.::

    http("Getting issues")
      .get("https://github.com/gatling/gatling/issues?milestone=1&state=open")

* or pass query parameters one by one to the method named ``queryParam(key: Expression[String], value: Expression[Any])``, e.g.::

    http("Getting issues")
      .get("https://github.com/gatling/gatling/issues")
      .queryParam("milestone", "1")
      .queryParam("state", "open")

Of course, you can use :ref:`Gatling Expression Language (EL) <el>` to make those values dynamic based on data in the virtual user's session::

  http("Value from session example")
    .get("https://github.com/gatling/gatling")
    // Global use case
    .queryParam("myKey", "${sessionKey}")
    // If the query parameter key and the session are the same
    .queryParam("myKey") // Equivalent to queryParam("myKey", "${myKey}")

If you'd like to specify a query parameter without value, you have to use ``queryParam("key", "")``::

  // GET https://github.com/gatling/gatling?myKey
  http("Empty value example").get("https://github.com/gatling/gatling").queryParam("myKey", "")

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]])``::

  multivaluedQueryParam("multi", "${foo}")) // where foo is the name of a Seq Session attribute
  multivaluedQueryParam("multi", Seq("foo", "bar")))
  multivaluedQueryParam("multi", session => Seq("foo", "bar")))

If you want to add multiple query parameters at once, there are two suitable methods:

* ``queryParamSeq(seq: Expression[Seq[(String, Any)]])``

::

  http("Getting issues")
    .get("https://github.com/gatling/gatling/issues")
    .queryParamSeq(Seq(("milestone", "1"), ("state", "open")))

* ``queryParamMap(map: Expression[Map[String, Any]])``

::

  http("Getting issues")
    .get("https://github.com/gatling/gatling/issues")
    .queryParamMap(Map("milestone" -> "1", "state" -> "open"))

.. note:: As all method parameters are ``Expression[T]``, i.e. 'key' parameter is an ``Expression[String]`` and so on, if you have more specific needs you can also provide an arbitrary ``Expression[T]``, i.e. a ``Session => Validation[T]`` function.
          This function will be evaluated against the user session every time this one pass through it.
          For a deeper look at `Expression` see dedicated section :ref:`here <expression>`.

.. _http-request-headers:

HTTP Headers
============

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


.. note:: Headers keys are defined as constants usable in the scenario, for example: ``HttpHeaderNames.ContentType``.
          You can find a list of the predefined constants `here <https://github.com/gatling/gatling/blob/master/gatling-http/src/main/scala/io/gatling/http/Headers.scala>`_.

.. note::
  There are two handful methods to help you set the required headers for JSON and XML requests:

  * ``http(...).get(...).asJSON`` is equivalent to::

      http(...).get(...)
        .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationJson)
        .header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationJson)

  * ``http(...).get(...).asXML`` is equivalent to::

      http(...).get(...)
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationXml)
      .header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationXml)

.. note:: Headers can also be defined on the ``HttpProtocol``.

.. _http-request-signature:

Signature Calculator
====================

You might want to generate some `HMAC <http://en.wikipedia.org/wiki/Hash-based_message_authentication_code>`_ header based on other request information: url, headers and/or body.
This can only happen after Gatling has resolved the request, e.g. computed the body based on a template.

Gatling exposes AsyncHttpClient's ``SignatureCalculator`` API::

  public interface SignatureCalculator {
    void calculateAndAddSignature(Request request,
                                  RequestBuilderBase<?> requestBuilder);
  }

``request`` is the immutable object that's been computed so far, ``requestBuilder`` is the mutable object that will be used to generate the final request.

So, basically, you have to read the proper information from the ``url`` and ``request`` parameters, compute the new information out of them, such as a HMAC header, and set it on the ``requestBuilder``.

There's 3 ways to set a SignatureCalculator on a request::

  .signatureCalculator(calculator: SignatureCalculator)

  // use this signature if you want to directly pass a function instead of a SignatureCalculator
  .signatureCalculator(calculator: (Request, RequestBuilderBase[_]) => Unit)

  // use this signature if you need information from the session to compute the signature (e.g. user specific authentication keys)
  // does not work with an anonymous function as in the second signature
  .signatureCalculator(calculator: Expression[SignatureCalculator])

.. _http-request-authentication:

Authentication
==============

You can set the authentication methods at request level with these methods:

* ``basicAuth(username: Expression[String], password: Expression[String])``
* ``digestAuth(username: Expression[String], password: Expression[String])``
* ``ntlmAuth(username: Expression[String], password: Expression[String], ntlmDomain: Expression[String], ntlmHost: Expression[String])``
* ``authRealm(realm: Expression[com.ning.http.client.Realm])``

::

  http("My BASIC secured request").get("http://my.secured.uri").basicAuth("myUser", "myPassword")

  http("My DIGEST secured request").get("http://my.secured.uri").digestAuth("myUser", "myPassword")


.. note:: Authentication can also be defined on the ``HttpProtocol``.

.. _http-request-outgoing-proxy:

Outgoing Proxy
==============

You can tell Gatling to use a proxy to send the HTTP requests.
You can optionally set a different port for HTTPS and credentials::

  http("Getting issues")
    .get("https://github.com/gatling/gatling/issues")
    .proxy(Proxy("myProxyHost", 8080).httpsPort(8143).credentials("myUsername","myPassword"))

.. note:: Proxy can also be defined on the ``HttpProtocol``.

.. _http-virtual-host:

Virtual Host
============

.. _http-request-virtual-host:

You can tell Gatling to override the default computed virtual host with the method ``virtualHost(virtualHost: Expression[String])``::

  // GET https://mobile.github.com/gatling/gatling instead of GET https://www.github.com/gatling/gatling
  http("Getting issues")
    .get("https://www.github.com/gatling/gatling/issues")
    .virtualHost("mobile")

.. note:: Virtual Host can also be defined on the ``HttpProtocol``.

HTTP Checks
===========

.. _http-request-check:

You can add checks on a request::

  http("Getting issues")
    .get("https://www.github.com/gatling/gatling/issues")
    .check(...)

For more information, see the :ref:`HTTP Checks reference section <http-check>`.

.. _http-request-ignore-default-checks:

For a given request, you can also disable common checks that were defined on the ``HttpProtocol`` with ``ignoreDefaultChecks``::

  http("Getting issues")
    .get("https://www.github.com/gatling/gatling/issues")
    .ignoreDefaultChecks

FollowRedirect
==============

.. _http-request-disable-follow-redirect:

For a given request, you can use ``disableFollowRedirect``, just like it can be done globally on the ``HttpProtocol``::

  http("Getting issues")
    .get("https://www.github.com/gatling/gatling/issues")
    .disableFollowRedirect

.. _http-request-silencing:

Silencing
=========

See :ref:`silencing protocol section <http-protocol-silencing>` for more details.

.. _http-request-silent:

You can then make the request *silent*: ::

  http("Getting issues")
    .get("https://www.github.com/gatling/gatling/issues")
    .silent

.. _http-request-notsilent:

You might also want to do the exact opposite, typically on a given resource while resources have been globally turned silent at protocol level::

  .resources(
    http("Gatling Logo")
    .get("http://gatling.io/assets/images/img1.png")
    .notSilent
  )

.. _http-post-parameters:

POST Parameters
===============

POST requests can have parameters defined in their body.
This is typically used for form submission, where all the values are stored as POST parameters in the body of the request.

To add such parameters to a POST request, you must use the method ``formParam(key: Expression[String], value: Expression[Any])`` which is actually the same as ``queryParam`` in **terms of usage** (it has the same signatures).

::

  http("My Form Data")
    .post("my.form-action.uri")
    .formParam("myKey", "myValue")

As for ``queryParam`` you have two methods to add multiple parameters at once:

* ``formParamSeq(seq: Expression[Seq[(String, Any)]])``

::

  http("My Form Data")
    .post("my.form-action.uri")
    .formParamSeq(Seq(("myKey", "myValue"), ("anotherKey", "anotherValue")))

* ``formParamMap(map: Expression[Map[String, Any]])``

::

  http("My Form Data")
    .post("my.form-action.uri")
    .formParamMap(Map("myKey" -> "myValue", "anotherKey" -> "anotherValue"))

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multivaluedFormParam(key: Expression[String], values: Expression[Seq[Any]])``::

  multiValuedFormParam("omg", "${foo}")) // where foo is the name of a Seq Session attribute
  multiValuedFormParam("omg", List("foo", "bar")))
  multiValuedFormParam("omg", session => List("foo", "bar")))

The method ``formParam`` can also take directly an `HttpParam` instance, if you want to build it by hand.

.. _http-multipart-form:

Multipart Form
==============

This applies only for POST requests. When you find forms asking for text values and a file to upload (usually an email attachment), your browser will send a multipart encoded request.

To define such a request, you have to add the parameters as stated above, and the file to be uploaded at the same time with the following method: ``formUpload(name: Expression[String], filePath: Expression[String])``.

The uploaded file must be located in ``user-files/request-bodies``. The ``Content-Type`` header will be set to ``multipart/form-data`` and the file added in addition to the parameters.

One can call ``formUpload()`` multiple times in order to upload multiple files.
::

  http("My Multipart Request")
    .post("my.form-action.uri")
    .formParam("myKey", "myValue")
    .formUpload("myKey2", "myAttachment.txt")

.. note:: The MIME Type of the uploaded file defaults to ``application/octet-stream`` and the character set defaults to the one configured in ``gatling.conf`` (``UTF-8`` by default).
          Don't forget to override them when needed.

.. note:: There is a helpful method to help you deal with multipart form requests: ``asMultipartForm``.
          It is equivalent to ``header(HttpHeaderNames.ContentType, HttpHeaderValues.MultipartFormData)``.
          If you use ``formUpload`` the header is automatically set for you.


.. _http-request-body:

Request Body
============

You can add a full body to an HTTP request with the dedicated method ``body(body)``, where body can be:

.. _http-request-body-rawfile:

* ``RawFileBody(path: Expression[String])`` where path is the location of a file that will be uploaded as is

``RawFileBody`` lets you pass a raw file that will be sent as is.
Over regular HTTP, Gatling can optimise sending such a body and directly stream from the file to the socket, without copying in memory.
Of course, this optimisation is disabled over HTTPS, as bytes have to be encoded, i.e. loaded in memory.::

  // myFileBody.json is a file that contains
  // { "myContent": "myHardCodedValue" }
  .body(RawFileBody("myFileBody.json")).asJSON

.. _http-request-body-elfile:

* ``ELFileBody(path: Expression[String])`` where path is the location of a file whose content will be parsed and resolved with Gatling EL engine

Here, the file content is parsed and turned into a Gatling EL expression.
Of course, it can't be binary.::

  // myFileBody.json is a file that contains
  // { "myContent": "${myDynamicValue}" }
  .body(ELFileBody("myFileBody.json")).asJSON

.. _http-request-body-string:

* ``StringBody(string: Expression[String])``

Here, you can pass a raw String, a Gatling EL String, or an Expression function.::

    .body(StringBody("""{ "myContent": "myHardCodedValue" }""")).asJSON

    .body(StringBody("""{ "myContent": "${myDynamicValue}" }""")).asJSON

    .body(StringBody(session => """{ "myContent": """" + someGenerator(session) + """" }""")).asJSON

.. _http-request-body-bytes:

* ``ByteArrayBody(bytes: Expression[Array[Byte]])``

.. _http-request-body-stream:

Here, you can pass bytes instead of text.

* ``InputStreamBody(stream: Expression[InputStream])``

Here, you can pass a Stream.

.. note:: When you pass a path, Gatling searches first for an absolute path in the classpath and then in the ``request-bodies`` directory.

Note that one can take full advantage of Scala 2.10 macros for writing template directly in Scala compiled code instead of relying on a templating engine.
See `Scala 2.10 string interpolation <(http://docs.scala-lang.org/overviews/core/string-interpolation.html>`_ and `Fastring <https://github.com/Atry/fastring>`_.

For example::

  object Templates {
    val template: Expression[String] = (session: Session) =>
    for {
      foo <- session("foo").validate[String]
      bar <- session("bar").validate[String]
    } yield s"""{
      foo: $foo,
      bar: $bar
    }"""
  }

.. note:: For simple use cases, prefer EL strings or based files, for more complex ones where programming capability is required, prefer String interpolation or Fastring.

.. _http-request-body-parts:

Multipart Request
=================

You can add a multipart body to an HTTP request and add parts with the dedicated method ``bodyPart(bodyPart)``, where bodyPart can be:

* ``RawFileBodyPart(path: Expression[String])``
* ``RawFileBodyPart(name: Expression[String], path: Expression[String])``

where path is the location of a file that will be uploaded as is.

Similar to :ref=`RawFileBody <http-request-body-rawfile>`.

* ``ELFileBodyPart(path: Expression[String])``
* ``ELFileBodyPart(name: Expression[String], path: Expression[String])``

where path is the location of a file whose content will be parsed and resolved with Gatling EL engine.

Similar to :ref=`ELFileBody <http-request-body-elfile>`.

* ``StringBodyPart(string: Expression[String])``
* ``StringBodyPart(name: Expression[String], string: Expression[String])``

Similar to :ref=`StringBody <http-request-body-string>`.

* ``ByteArrayBodyPart(bytes: Expression[Array[Byte])``
* ``ByteArrayBodyPart(name: Expression[String], bytes: Expression[Array[Byte])``

Similar to :ref=`ByteArrayBody <http-request-body-bytes>`.

Once bootstrapped, BodyPart has the following methods for setting additional optional information:

* ``contentType(contentType: String)``
* ``charset(charset: String)``, part of of ``Content-Type`` header. If not set, defaults to the one from ``gatling.conf`` file.
* ``fileName(fileName: Expression[String])``, part of the *Content-Disposition* header.
* ``dispositionType(contentId: String)``, part of the ``Content-Disposition`` header. If not set, defaults to ``form-data``.
* ``contentId(contentId: Expression[String])``
* ``transferEncoding(transferEncoding: String)``

.. _http-request-processor:

Request processor
=================

You might want to process the request body before it's being sent to the wire.

``processRequestBody(processor: Body => Body)``: takes a ``Body => Body``

Gatling ships two built-ins:

* ``gzipBody``: compress the request body with GZIP
* ``streamBody``: turn the body into a stream

.. _http-response-processor:

Response processors
===================

Similarly, one might want to process the response before it's passed to the checks pipeline.

``transformResponse(responseTransformer: PartialFunction[Response, Response])``: takes a ``Response => Response``

The example below shows how to decode some Base64 encoded response body::

  import com.ning.http.util.Base64
  import io.gatling.http.response._
  import java.nio.charset.StandardCharsets.UTF_8

  // ignore when response isn't received (e.g. when connection refused)
  .transformResponse { case response if response.isReceived =>
    new ResponseWrapper(response) {
      override val body = ByteArrayResponseBody(Base64.decode(response.body.string), UTF_8)
    }
  }

.. _http-resources:

Resources
=========

Gatling allow to fetch resources in parallel in order to emulate the behavior of a real web browser.

At the request level you can use the ``resources(res: AbstractHttpRequestBuilder[_]*)`` method.

For example::

  http("Getting issues")
    .get("https://www.github.com/gatling/gatling/issues")
    .resources(
      http("api.js").get("https://collector-cdn.github.com/assets/api.js"),
      http("ga.js").get("https://ssl.google-analytics.com/ga.js")
    )

.. _http-chunksdiscard:

Response chunks discarding
==========================

``disableResponseChunksDiscarding`` works just like the :ref:`protocol level parameter <http-protocol-chunksdiscard>`, except that it targets this request only.
