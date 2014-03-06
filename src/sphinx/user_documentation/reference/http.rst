####
HTTP
####

As the first protocol module of Gatling, Gatling HTTP allows you to load test web applications, web services or websites. It supports HTTP and HTTPS with almost every existing features of common browsers.

HTTP requests are declared in ``exec()`` method, eg::

	exec( http(...).queryParam(...) )

.. _http-tuning:

Preamble: a Word on Tuning
==========================

Connection sharing
------------------

First of all, be aware that since 1.5.0, virtual users behave by default as distinct user agents, in the sense that they have their own connections. This behavior is the default as we believe most people want to emulate browsers. This behavior is also the only way to have SSL session tracking work.

If this is not your use case and want your virtual users to share the same connections, you might want to change this behavior. If so, please have a look at the :ref:`shareConnections <share-connections>` HTTP protocol configuration.

Connection closing
------------------

Gatling doesn't close the connections used by a given user when the user exits the simulation.

Connections are freed on 2 events:

* Gatling shutdown
* The connection stays idle for too long in the connection pool. This time out can be tuned in ``gatling.conf`` with the ``idleConnectionInPoolTimeoutInMs`` parameter, default value is 60.

OS Tuning
---------

Depending on your OS, you might have to tweak a few options so that you can massively open new sockets and reach heavy load.

For example, on X platforms, you imperatively have to increase the number of files you can open with ``ulimit -n``.

Under Mac OS/X Lion you need to run the following commands in order to "unbuckle the belts"::

	sudo sysctl -w kern.maxfilesperproc=200000
	sudo sysctl -w kern.maxfiles=200000
	sudo sysctl -w net.inet.ip.portrange.first=1024

You could also have to increase your `ephemeral port range <http://www.ncftp.com/ncftpd/doc/misc/ephemeral_ports.html>`_ or tune your TCP time out so that they expire faster.

Here's also a good entry point: https://github.com/0xdata/h2o/wiki/EC2-hosts-configuration-and-tuning

Protocol Configuration
======================

As every protocol in Gatling, the HTTP protocol can be configured for a scenario. This is done thanks to the following statements::

	val httpConf = httpConfig.baseURL("http://my.website.tld")
	...
	setUp(scn.protocolConfig(httpConf))

.. _http-base-url:

Base URL
--------

As you may have seen in the previous example, you can set a base URL. This base URL will be prepended to all urls that does not start with ``http``, eg::

	val httpConf = httpConfig.baseURL("http://my.website.tld")

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

Stressing several servers with client based load balancing *(since 1.3.0)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you want to stress several servers at the same time, to bypass a load-balancer for example, you can use the method named ``baseURLs(String*)``::

	val httpConf = httpConfig.baseURLs("http://my1.website.tld", "http://my2.website.tld", "http://my3.website.tld")

The selection of the URL is made at each request, using the ``Random`` generator.

Outgoing Proxy
--------------

If your simulation must be run behind a proxy, you can tell Gatling to use a proxy to send the HTTP requests.

You can set the HTTP proxy, on optional HTTPS proxy and optional credentials for the proxy::

	val httpConf = httpConfig.proxy("myProxyHost", 8080)
	                         .httpsPort(8143)
	                         .credentials("myUsername","myPassword")

.. _http-follow-redirects:

Follow Redirects *(since 1.1.0)*
--------------------------------

Some requests lead to a redirect response from the server.
Since 1.2.0, this feature is enabled by default.

With this configuration, Gatling will automatically follow redirects until reaching a non 301/302 status code. This checks will be applied on the last page of the redirect chain (ie the final page displayed to the user).

To disable this feature, just add ``.disableFollowRedirect`` to an HTTP Protocol Configuration definition.

::

	val httpConf = httpConfig.disableFollowRedirect

	setUp(scn.users(600).ramp(200).protocolConfig(httpConf))

Common Headers *(since 1.2.0)*
------------------------------

Some HTTP headers can be mutualized in the Http Protocol configuration.
Of course, declaring at request level a header that's already been defined at protocol level will override the value.

::

	.acceptHeader("foo")
	.acceptCharsetHeader("foo")
	.acceptEncodingHeader("foo")
	.acceptLanguageHeader("foo")
	.userAgentHeader("foo")

.. note:: Those common headers are automatically computed by the Recorder.

Automatic Referer *(since 1.2.0)*
---------------------------------

The ``Referer`` HTTP header can be automatically computed.
This feature is enabled by default.

To disable this feature, just add ``.disableAutomaticReferer`` to an HTTP Protocol Configuration definition.

.. _http-warmup:

Automatic warm up *(since 1.2.0)*
---------------------------------

The Java/NIO engine start up introduces an overhead on the first request to be executed.
In order to compensate this effect, Gatling automatically performs a request to http://gatling-tool.org.

To disable this feature, just add ``.disableWarmUp`` to an HTTP Protocol Configuration definition.
To change the warm up url, just add ``.warmUp("newUrl")``.

.. _dumping-custom-data:

Dumping custom data *(since 1.2.4)*
-----------------------------------

Some people might want more data than what Gatling normally dumps in the ``simulation.log`` file.

httpConfig provides hooks for dumping extra data: one for request and one for response.

The extra data will be appended to the relative records in the ``simulation.log`` file and reports generation will ignore them. It's up to the user to build his own analysis system for them.

``requestInfoExtractor(function: (Request => List[String]))``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following built-in functions are automatically available:

* requestUrl
* requestRawUrl

``responseInfoExtractor(function: (Response => List[String]))``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following built-in functions are automatically available:

* responseStatusCode
* responseStatusText
* responseContentType
* responseUri

``disableResponseChunksDiscarding`` *(since 1.3.0)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Beware that, as an optimization, Gatling doesn't pile up response chunks unless a check is defined on the response body.
If you don't want to set up a check and still want to get the response body from ``responseInfoExtractor``, you have to disable this feature. Just add ``.disableResponseChunksDiscarding`` to an HTTP Protocol Configuration definition.

.. _share-connections:

``shareConnections`` *(since 1.5.0)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, virtual users don't share connections. Use this option to change this behavior.

Cookies handling
================

Gatling transparently handles cookies, just like a browser would (they are actually stored in the user Session).

.. note:: Please note the following limitations:

	* Idle expiration is not supported, as having an idle user during a stress test doesn't make sense. However, explicit expiration when the server returns a cookie with an expiration date in the past is perfectly supported.
	* Gatling currently doesn't support ``HttpOnly`` property, as it currently handles indifferently http and non-http requests.

.. _http-caching:

Caching *(since 1.3.0)*
=======================

Gatling supports basic caching, meaning that:

* Cache-Control different from no-cache is interpreted as cache forever
* Future Expires date is interpreted as cache forever
* ETag is not supported

To disable this feature, just add ``.disableCaching`` to an HTTP Protocol Configuration definition.

Declaring an HTTP request
=========================

Method and URI
--------------

If you know HTTP protocol, you certainly know that for a request to be sent, there are mandatory parameters to be set. The first ones are the Method and the URI of the request.

Gatling currently supports 5 of the 8 methods of the HTTP protocol:

* HEAD - Used to **head** information stored at the URI
* GET - Used to **get** information stored at the URI
* POST - Used to **post** information to an HTTP server
* PUT - Used to **update** existing information at the URI
* DELETE - Used to **delete** existing information at the URI

.. note:: These methods are the ones used in REST webservices and RESTful applications; thus, such services can be tested with Gatling.

This is how an HTTP request is declared::

	// general structure of an HTTP request
	http("Name of the request").method(URI)
	    
	// concrete examples
	http("Retrieve home page").get("https://github.com/excilys/gatling")
	http("Login").post("https://github.com/session")

.. _http-query-parameters:

Query Parameters
----------------

To send information to a web server, frameworks and developers use query parameters, you can find them after the ``?`` of an URI::

    https://github.com/excilys/gatling/issues?milestone=1&state=open

Here the query parameters are:

* *milestone=1* : the key is ``milestone`` and its value is ``1``
* *state=open* : the key is ``state`` and its value is ``open``

To define the query parameters of an HTTP request, you can use the method named ``queryParam(key: String, value: String)``, eg::

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

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multiValuedQueryParam``::

	multiValuedQueryParam("omg", "foo")) // where foo is the name of a Seq Session attribute
	multiValuedQueryParam("omg", List("foo")))
	multiValuedQueryParam("omg", session => List("foo")))

HTTP Headers
------------

HTTP protocol uses headers to exchange information between client and server that is not part of the message (stored in the body of the request, if there is one). Gatling HTTP allows you to specify any header you want to with the ``header`` and ``headers`` methods. Here are some examples::

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

.. note:: headers keys are defined as constants usable in the scenario, for example: ``CONTENT_TYPE``. You can find a list of the predefined constants `here <http://gatling-tool.org/api/gatling-http/#com.excilys.ebi.gatling.http.Predef$>`_.

.. note:: There are two handful methods to help you deal with JSON requests and XML requests: ``asJSON`` and ``asXML``. They are equivalent to ``header(CONTENT_TYPE, APPLICATION_JSON).header(ACCEPT, APPLICATION_JSON)`` and ``header(CONTENT_TYPE, APPLICATION_XML).header(ACCEPT, APPLICATION_XML)`` respectively.

BASIC Authentication
--------------------

HTTP provides two authentication methods to secure URIs: 

* BASIC 
* DIGEST

Gatling supports BASIC authentication.

To add authentication headers to a request, you must use the method ``basicAuth(username: String, password: String)`` as follows::

	http("My secured request").get("http://my.secured.uri").basicAuth("myUser", "myPassword")

Method specific methods
=======================

*Sorry for this bizarre title ;-)*

Adding a body to a request
--------------------------

When the request's method is POST, PUT or DELETE, it can contain a body.

.. note:: The Content-Length HTTP header will be overridden by gatling for each request since the body length is calculated at run time of the simulation.

body(body: String)
^^^^^^^^^^^^^^^^^^

This one one lets you define a body in place with a String::

	http("String body").post("my.post.uri")
	  .body("""{ "myContent": "myValue" }""").asJSON

.. note:: In Scala, you can use escaped strings with this notation: ``"""my "non-escaped" string"""``. This simplifies the writing and reading of strings containing special characters.

fileBody(fileName: String)
^^^^^^^^^^^^^^^^^^^^^^^^^^

This one lets you include a file as the body of the request. This file must be located in the `user-files/request-bodies` folder of your Gatling directory.

.. highlight:: js

::

	/*user-files/request-bodies/myFile.json*/
	{ "myContent": "myValue" }

.. highlight:: scala

::

	/* Scenario */
	http("File body").post("my.post.uri")
	  .fileBody("myFile.json").asJSON


fileBody(templateFileName: String, valuesToReplace: Map[String, String])
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This one lets you include the output of a template as the body of the request. The template file must be located in ``user-files/request-bodies`` and be an SSP file, eg: ``myTemplate.ssp``. You can find more information about SSP on the `Scalate website <http://scalate.fusesource.org/documentation/ssp-reference.html>`_.

.. highlight:: js

::

	/* user-files/request-bodies/myTemplate.ssp */
	{ "myContent": "<%= value %>" }

.. highlight:: scala

::

	/* Scenario */
	http("Template Body").post("my.post.uri")
	  .fileBody("myTemplate", Map("value" -> "myValue")).asJSON

.. note:: Instead of ``"myValue"```, you can use ELs to insert session values in your template: ``Map("value" -> "${mySessionKey}")``

.. _http-byte-array-body:

byteArrayBody (byteArray : (Session) => Array[Byte]) *(since 1.3.0)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This one allows for injection of a byte array body in the http request during a simulation. The method accepts a callback function that returns an Array[Byte]. It is useful for testing APIs that accept binary data (i.e protocol buffers) and you want to generate the data dynamically in memory on the fly. An example simulation where the binary data was loaded beforehand in Redis can be viewed `here <https://gist.github.com/4345254>`_.

POST Parameters
---------------

POST requests can have parameters defined in their body. This is typically used for form submission, where all the values are stored as POST parameters in the body of the request.

To add such parameters to a POST request, you must use the method ``param(key: String, value: String)`` which is actually the same as ``queryParam`` in **terms of usage** (it has the same signatures).

::

	http("My Form Data").post("my.form-action.uri")
	  .param("myKey", "myValue")

If you'd like to pass multiple values for your parameter, but all at once, you can use ``multiValuedParam``::

	multiValuedParam("omg", "${foo}")) // where foo is the name of a Seq Session attribute
	multiValuedParam("omg", List("foo")))
	multiValuedParam("omg", session => List("foo")))

Multipart encoded requests
--------------------------

This applies only for POST requests. When you find forms asking for text values and a file to upload (usually an email attachment), your browser will send a multipart encoded request.

To define such a request, you have to add the parameters as stated above, and the file to be uploaded at the same time with the following method::

	upload(paramKey, fileName, mimeType: String, charset: String)

``paramKey`` and ``fileName`` can be String, EL or Session => String. 

The uploaded file must be located in ``user-files/request-bodies``. The Content-Type header will be set to "multipart/form-data" and the file added in addition to the parameters.

One can call ``upload`` multiple times in order to upload multiple files.
::

	http("My Multipart Request").post("my.form-action.uri")
	  .param("myKey", "myValue")
	  .upload("myKey2", "myAttachment.txt")

.. note:: The MIME Type of the uploaded file defaults to ``application/octet-stream`` and the character set defaults to the one configured in ``gatling.conf`` (``UTF-8`` by default). Don't forget to override them when needed.