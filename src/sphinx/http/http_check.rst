.. _http-check:

######
Checks
######

Concepts
========

The Check API is used for 2 things:

* verifying that the response to a request matches expectations
* eventually capturing some elements in it.

Checks are performed on a request with the ``check`` method.
For example, on an HTTP request::

  http("My Request").get("myUrl").check(status.is(200))

One can of course perform multiple checks::

  http("My Request").get("myUrl").check(status.not(404), status.not(500)))


This API provides a dedicated DSL for chaining the following steps:

1. :ref:`defining the check type <http-check-type>`
2. :ref:`extracting <http-check-extracting>`
3. :ref:`transforming <http-check-transform>`
4. :ref:`validating <http-check-validating>`
5. :ref:`saving <http-check-saving>`

.. note:: By default, Gatling follows redirects (can be disabled in the :ref:`protocol <http-protocol-redirect>`).
          If this behavior is enabled, checks will ignore intermediate responses and will target the landing response.

.. _http-check-type:

Defining the check type
=======================

The HTTP Check implementation provides the following built-ins:

HTTP status
-----------

.. _http-check-status:

* ``status``

Targets the HTTP response status code.

.. note:: A status check is automatically added to a request when you don't specify one.
          It checks that the HTTP response has a 2XX or 304 status code.

Page location
-------------

.. _http-check-current-location:

* ``currentLocation``

Targets the current page absolute URL.
Useful when following redirects in order to check if the landing page is indeed the expected one.

.. _http-check-current-location-regex:

* ``currentLocationRegex(pattern)``

Same as above, but *pattern* is used to apply a regex on the current location.

By default, it can extract 0 or 1 capture group, so the extract type is ``String``.

One can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step::

  currentLocationRegex(pattern).ofType[T]

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups::

  currentLocationRegex("http://foo.com/bar?(.*)=(.*)").ofType[(String, String)]

HTTP header
-----------

.. _http-check-header:

* ``header(headerName)``

Targets the HTTP response header of the given name.
*headerName* can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

.. _http-check-header-regex:

* ``headerRegex(headerName, pattern)``

Same as above, but *pattern* is used to apply a regex on the header value.

.. note:: The header names are available as constants in the DSL, accessible from the ``HttpHeaderNames`` object, e.g. ``HttpHeaderNames.ContentType``.

.. note:: ``Location`` header value is automatically decoded when performing a check on it.

By default, it can extract 0 or 1 capture group, so the extract type is ``String``.

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

Return the full response body String.
Note that this can be matched against content from the the filesystem using :ref:`RawFileBody <http-request-body-rawfile>` or :ref:`ELFileBody <http-request-body-elfile>`.

.. _http-check-body-bytes:

* ``bodyBytes``

Return the full response body byte array.

.. _http-check-substring:

* ``substring(expression)``

Scans for the indices of a given substring inside the body string.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

::

  substring("foo")                           // same as substring("foo").find.exists
  substring("foo").findAll.saveAs("indices") // saves a Seq[Int]
  substring("foo").count.saveAs("counts")    // saves the number of occurrences of foo


.. note:: Typically used for checking the presence of a substring, as it's more CPU efficient than a regular expression.

.. _http-check-regex:

* ``regex(expression)``

Defines a Java regular expression to be applied on any text response body.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

It can contain multiple capture groups.

::

  regex("""<td class="number">""")
  regex("""<td class="number">ACC${account_id}</td>""")
  regex("""/private/bank/account/(ACC[0-9]*)/operations.html""")

.. note:: In Scala, you can use escaped strings with this notation: ``"""my "non-escaped" string"""``.
          This simplifies the writing and reading of regular expressions.

By default, it can extract 0 or 1 capture group, so the extract type is ``String``.

You can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step::

  regex(expression).ofType[T]

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups::

  regex("foo(.*)bar(.*)baz").ofType[(String, String)]

.. _http-check-xpath:

* ``xpath(expression, namespaces)``

Defines an XPath 1.0 expression to be applied on an XML response body.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

*namespaces* is an optional List of couples of (prefix, uri)

::

  xpath("//input[@id='text1']/@value")
  xpath("//foo:input[@id='text1']/@value", List("foo" -> "http://foo.com"))

.. note:: XPath only works on well formed XML documents, which regular HTML is not (while XHTML is).
          If you're looking for path expression for matching HTML documents, please have a look at our :ref:`CSS selectors support<http-check-css>`.

.. note:: You can also use ``vtdXpath(xpathExpression: Expression[String])``, this check uses VTD as the XPath engine,
          it is available as a `separate module <https://github.com/gatling/gatling-vtd>`_.

.. _http-check-jsonpath:

* ``jsonPath(expression)``

JsonPath is a XPath-like syntax for JSON. It was specified by Stefan Goessner.
Please check `Goessner's website <http://goessner.net/articles/JsonPath>`_ for more information about the syntax.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

::

  jsonPath("$..foo.bar[2].baz")


By default, it extracts ``String``\ s, so JSON values of different types get serialized.

You can define an different type with the ``ofType[T]`` extra step::

  jsonPath(expression).ofType[T]

Gatling provides built-in support for the following types:

* String
* Boolean
* Int
* Long
* Double
* Float
* Seq (JSON array)
* Map (JSON object)
* Any


The example below shows how to extract Ints

.. code-block:: json

  {
    "foo": 1,
    "bar" "baz"
  }

::

  jsonPath("$..foo").ofType[Int] // will match 1

.. _http-check-jsonp-jsonpath:

* ``jsonpJsonPath(expression)``

Same as :ref:`jsonPath <http-check-jsonpath>` but for `JSONP <http://en.wikipedia.org/wiki/JSONP>`_.

.. _http-check-css:

* ``css(expression, attribute)``

Gatling supports `CSS Selectors <http://jodd.org/doc/csselly>`_.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

*attribute* is an optional ``String``.
When filled, check is performed against the attribute value.
Otherwise check is performed against the node text content.

::

  css("article.more a", "href")

.. _http-check-checksum:

* ``md5`` and ``sha1``

Returns a checksum of the response body.
Checksums are computed efficiently against body parts as soon as they are received.
They are then discarded if not needed.

.. note:: checksums are computed against the stream of chunks, so the whole body is not stored in memory.

.. _http-check-extracting:

Extracting
==========

.. _http-check-find:

* ``find``

Returns the first occurrence. If the check targets more than a single element, ``find`` is identical to ``find(0)``.

.. note:: In the case where no extracting step is defined, a ``find`` is added implicitly.

Multiple results
----------------

* ``find(occurrence)``

Returns the occurrence of the given rank.

.. note:: Ranks start at 0.

.. _http-check-find-all:

* ``findAll``

Returns a List of all the occurrences.

.. _http-check-count:

* ``count``

Returns the number of occurrences.

``find(occurrence)``, ``findAll`` and ``count`` are only available on check types that might produce multiple results.
For example, ``status`` only has ``find``.

.. _http-check-transform:

Transforming
============

Transforming is an **optional** step for transforming the result of the extraction before trying to match or save it.

``transform(function)`` takes a ``X => X2`` function, meaning that it can only transform the result when it exists.

.. note:: You can also gain access to the ``Session`` and pass a ``(X, Session) => X2`` instead.

``transformOption(function)`` takes a ``Option[X] => Validation[Option[X2]]`` function, meaning that it gives full control over the extracted result, even providing a default value.

.. note:: You can also gain access to the ``Session`` and pass a ``(Option[X], Session) => Validation[X2]`` instead.

::

  transform(string => string + "foo")

  transformOption(extract => extract.orElse(Some("default"))).success)

.. _http-check-validating:

Validating
==========

.. _http-check-is:

* ``is(expected)``

Checks that the value is equal to the expected one, e.g.::

  status.is(200)

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a ``String``, it can also be a ``String`` using Gatling EL or an ``Expression[String]``.

.. _http-check-not:

* ``not(expected)``

Checks that the value is different from the expected one::

  status.not(500)

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a ``String``, it can also be a ``String`` using Gatling EL or an ``Expression[String]``.

.. _http-check-exists:

* ``exists``

Checks that the value exists and is not empty in case of multiple results::

  jsonPath("$..foo").exists

.. _http-check-not-exists:

* ``notExists``

Checks that the value doesn't exist and or is empty in case of multiple results::

  jsonPath("$..foo").notExists

.. _http-check-in:

* ``in(sequence)``

Checks that the value belongs to a given sequence or vararg::

  status.in(200, 304)

*sequence*

.. _http-check-optional:

* ``optional``

.. warning::
  ``optional`` used to be named ``dontValidate``. The old name has been deprecated, then removed in Gatling 2.1.

Always true, used for capture an optional value.

*expected* is a function that returns a sequence of values of the same type of the previous step (extraction or transformation).

.. note:: In the case where no verifying step is defined, a ``exists`` is added implicitly.

.. _http-check-saving:

Saving
======

``saveAs(key)``

Saving is an **optional** step for storing the result of the previous step (extraction or transformation) into the virtual user Session, so that it can be reused later.

*key* is a ``String``.

Putting it all together
=======================

To help you understand the checks, here is a list of examples:

::

  check(regex("""https://(.*)""").count.is(5))

Verifies that there are exactly 5 HTTPS links in the response.

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

::

  bodyString.is(RawFileBody("expected_response.json"))

Verifies that the response body matches the file ``user-files/bodies/expected_response.json``

::

  bodyString.is(ELFileBody("expected_template.json"))

Verifies that the response body matches the content of the file ``user-files/bodies/expected_template.json`` resolved with :ref:`Gatling Expression Language (EL) <el>`.
