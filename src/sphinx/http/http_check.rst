.. _http-check:

######
Checks
######

Concepts
========

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
=======================

The HTTP Check implementation provides the following built-ins:

HTTP status
-----------

.. _http-check-status:

* ``status``

Targets the HTTP response status code.

.. note:: A status check is automatically added to a request when you don't specify one.
          It checks that the HTTP response has a 2XX status code.

.. _http-check-current-location:

* ``currentLocation``

Targets the current page absolute URL.
Useful when following redirects in order to check if the landing page is indeed the expected one.


HTTP header
-----------

.. _http-check-header:

* ``header(headerName)``

Targets the HTTP response header of the given name.
*headerName* can be a simple String, an evaluable String containing expression, or an Expression[String].

.. _http-check-header-regex:

* ``headerRegex(headerName, pattern)``

Same than above, but *pattern* is used to apply a regex on the header value.

.. note:: The header names are available as constants in the DSL. They all are written in upper case and words are separated with underscores, eg: CONTENT_TYPE

.. note:: ``Location`` header value is automatically decoded when performing a check on it

By default, it can extract 0 or 1 capture group, so the extract type is ``String``\ s.

One can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step::

  headerRegex(headerName, pattern).ofType[T]

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups::

  headerRegex("FOO", "foo(.*)bar(.*)baz").ofType[(String, String)]

.. _http-check-response-body:

HTTP response body
------------------

HTTP checks are performed in the order of HTTP element precedence: first status, then headers, then response body.

Beware that, as an optimization, Gatling doesn't pile up response chunks unless a check is defined on the response body.

.. _http-check-response-time:

* ``responseTimeInMillis``

Returns the response time of this request in milliseconds = the time between starting to send the request and finishing to receive the response.

.. _http-check-latency:

* ``latencyInMillis``

Returns the latency of this request in milliseconds = the time between finishing to send the request and starting to receive the response.

.. _http-check-body-string:

* ``bodyString``

Return the full response body.

.. _http-check-regex:

* ``regex(expression)``

Defines a Java regular expression to be applied on any text response body.

*expression* can be a simple String, a String containing an expression, or an Expression[String].

It can contain multiple capture group.

::

	regex("""<td class="number">""")
	regex("""<td class="number">ACC${account_id}</td>""")
	regex("""/private/bank/account/(ACC[0-9]*)/operations.html""")

.. note:: In Scala, you can use escaped strings with this notation: ``"""my "non-escaped" string"""``.
          This simplifies the writing and reading of regular expressions.

By default, it can extract 0 or 1 capture group, so the extract type is ``String``\ s.

One can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step::

  regex(expression).ofType[T]

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups::

  regex("foo(.*)bar(.*)baz").ofType[(String, String)]

* ``xpath(expression, namespaces)``

.. _http-check-xpath:

Defines an XPath 1.0 expression to be applied on an XML response body.

*expression* can be a simple String, an evaluatable String containing expression, or an Expression[String].

*namespaces* is an optional List of couples of (prefix, uri)

::

	xpath("//input[@id='text1']/@value")
	xpath("//foo:input[@id='text1']/@value", List("foo" -> "http://foo.com"))

.. note:: You can also use vtdXpath(xpathExpression: String), this check uses VTD as the XPath engine,
          it is available as a `separate module <https://github.com/excilys/gatling-vtd>`_.

* ``jsonPath(expression)``

.. _http-check-jsonpath:

Based on `Goessner's JsonPath <http://goessner.net/articles/JsonPath>`_.

*expression* can be a simple String, a String containing an EL expression, or an Expression[String].

::

	jsonPath("$..foo.bar[2].baz")

.. note:: In JSON, the root element has no name.
          This might be a problem when it's an array and one want to target its elements.
          As a workaround, Gatling names it ``_``.

By default, it extracts ``String``\ s, so JSON values of different types get serialized.

One can define an different type with the ``ofType[T]`` extra step::

  jsonPath(expression).ofType[T]

Gatling provides built-in support for the following types:

  * String
  * Int
  * Long
  * Double
  * Float
  * Seq (JSON array)
  * Map (JSON object)
  * Any

The example below shows how to extract Ints::

  jsonPath("$..foo").ofType[Int]

.. _http-check-jsonp-jsonpath:

* ``jsonpJsonPath(expression)``

Same as :ref:`jsonPath <http-check-jsonpath>` but for `JSONP <http://en.wikipedia.org/wiki/JSONP>`_.

.. _http-check-css:

* ``css(expression, attribute)``

Gatling supports `CSS Selectors <http://jodd.org/doc/csselly>`_.

*expression* can be a simple String, a String containing an EL expression, or a (Session => String) function.

*attribute* is an optional String.
When filled, check is performed against the attribute value.
Otherwise check is performed against the node text content.

.. _http-check-checksum:

* ``md5`` and ``sha1``

Returns a checksum of the response body.
Checksums are computed efficiently against body parts as soon as there's received.
Those are then discarded if not needed.

.. note:: checksums are computed against the stream of chunks, so the whole body is not stored in memory.

.. _http-check-extracting:

Extracting
==========

.. _http-check-find:

* ``find``: return the first occurrence

* ``find(occurrence)``: return the occurrence of the given rank

.. note:: Ranks start at 0.

.. _http-check-find-all:

* ``findAll``: return a List of all the occurrences

.. _http-check-count:

* ``count``: return the number of occurrences

find(occurrence), findAll and count are only available on check types that might produce multiple results.
For example, status only has find.

.. note:: In case of no extracting step is defined, a ``find`` is added implicitly.

.. _http-check-transforming:

Transforming
============

``transform(transformationFunction)``

Transforming is an **optional** step for transforming the result of the extraction before trying to match or save it.

*transformationFunction* is a function whose input is the extraction result and output is the result of your transformation.

::

	transform(string => string + "foo")

.. _http-check-verifying:

Verifying
=========

.. _http-check-is:

* ``is(expected)``

Checks that the value is equal to the expected one.

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).
In case of a String, it can also be a static String or a String with an EL expression.

.. _http-check-not:

* ``not(expected)``

Checks that the value is different from the expected one.

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).
In case of a String, it can also be a static String or a String with an EL expression.

.. _http-check-exists:

* ``exists``

Checks that the value exists and is not empty in case of multiple results.

.. _http-check-not-exists:

* ``notExists``

Checks that the value doesn't exist and or is empty in case of multiple results.

.. _http-check-in:

* ``in(sequence)``

Checks that the value belongs to a given sequence.

.. _http-check-whatever:

* ``dontValidte``

Always true, used for capture an optional value.

*expected* is a function that returns a sequence of values of the same type of the previous step (extraction or transformation).

.. note:: In case of no verifying step is defined, a `exists`` is added implicitly.

.. _http-check-saving:

Saving
======

``saveAs(key)``

Saving is an optional step for storing the result of the previous step (extraction or transformation) into the virtual user Session, so that it can be reused later.

*key* is a String

Putting it all together
=======================

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
