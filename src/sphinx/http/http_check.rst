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
For example, on an HTTP request:

.. includecode:: code/Checks.scala#status-is-200

One can of course perform multiple checks:

.. includecode:: code/Checks.scala#status-is-not-404-or-500

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

One can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step:

.. includecode:: code/Checks.scala#currentLocationRegex-ofType

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups:

.. includecode:: code/Checks.scala#currentLocationRegex-example

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

By default, it can extract 0 or 1 capture group, so the extract type is ``String``.

One can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step:

.. includecode:: code/Checks.scala#headerRegex-ofType

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups:

.. includecode:: code/Checks.scala#headerRegex-example

.. _http-check-response-body:

HTTP response body
------------------

HTTP checks are performed in the order of HTTP element precedence: first status, then headers, then response body.

Beware that, as an optimization, Gatling doesn't pile up response chunks unless a check is defined on the response body.

.. _http-check-response-time:

* ``responseTimeInMillis``

Returns the response time of this request in milliseconds = the time between starting to send the request and finishing to receive the response.

.. _http-check-body-string:

* ``bodyString``

Return the full response body String.
Note that this can be matched against content from the the filesystem using :ref:`RawFileBody <http-request-body-rawfile>` or :ref:`ElFileBody <http-request-body-elfile>`.

.. _http-check-body-bytes:

* ``bodyBytes``

Return the full response body byte array.

.. _http-check-substring:

* ``substring(expression)``

Scans for the indices of a given substring inside the body string.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

.. includecode:: code/Checks.scala#substring

.. note:: Typically used for checking the presence of a substring, as it's more CPU efficient than a regular expression.

.. _http-check-regex:

* ``regex(expression)``

Defines a Java regular expression to be applied on any text response body.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

It can contain multiple capture groups.

.. includecode:: code/Checks.scala#regex

.. note:: In Scala, you can use escaped strings with this notation: ``"""my "non-escaped" string"""``.

          This simplifies the writing and reading of regular expressions.

By default, it can extract 0 or 1 capture group, so the extract type is ``String``.

You can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step:

.. includecode:: code/Checks.scala#regex-ofType

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups:

.. includecode:: code/Checks.scala#regex-example

.. _http-check-xpath:

* ``xpath(expression, namespaces)``

Defines an XPath 1.0 expression to be applied on an XML response body.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

*namespaces* is an optional List of couples of (prefix, uri)

.. includecode:: code/Checks.scala#xpath

.. note:: XPath only works on well formed XML documents, which regular HTML is not (while XHTML is).
          If you're looking for path expression for matching HTML documents, please have a look at our :ref:`CSS selectors support<http-check-css>`.

.. note:: You can also use ``vtdXpath(xpathExpression: Expression[String])``, this check uses VTD as the XPath engine,
          it is available as a `separate module <https://github.com/gatling/gatling-vtd>`_.

.. _http-check-jsonpath:

* ``jsonPath(expression)``

JsonPath is a XPath-like syntax for JSON. It was specified by Stefan Goessner.
Please check `Goessner's website <http://goessner.net/articles/JsonPath>`_ for more information about the syntax.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

.. includecode:: code/Checks.scala#jsonPath

By default, it extracts ``String``\ s, so JSON values of different types get serialized.

You can define an different type with the ``ofType[T]`` extra step:

.. includecode:: code/Checks.scala#jsonPath-ofType

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


The example below shows how to extract Ints:

.. includecode:: code/Checks.scala
  :include: json-response,jsonPath-Int

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

.. includecode:: code/Checks.scala#css

You can define an different return type with the ``ofType[T]`` extra step:

.. includecode:: code/Checks.scala#css-ofType

Gatling provides built-in support for the following types:

* String
* Node

Specifying a ``Node`` let you perform complex deep DOM tree traversing, typically in a ``transform`` check step.
Node is a `Jodd Lagardo <http://jodd.org/doc/lagarto/>`_ DOM `Node <http://jodd.org/api/jodd/lagarto/dom/Node.html>`_.

* ``form(expression)``

This check takes a CSS selector and returns a ``Map[String, Seq[String]]`` of the form field values.

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

.. includecode:: code/Checks.scala
  :include: transform,transformOption

.. _http-check-validating:

Validating
==========

.. _http-check-is:

* ``is(expected)``

Checks that the value is equal to the expected one, e.g.:

.. includecode:: code/Checks.scala#is

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a ``String``, it can also be a ``String`` using Gatling EL or an ``Expression[String]``.

.. _http-check-not:

* ``not(expected)``

Checks that the value is different from the expected one:

.. includecode:: code/Checks.scala#not

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a ``String``, it can also be a ``String`` using Gatling EL or an ``Expression[String]``.

.. _http-check-exists:

* ``exists``

Checks that the value exists and is not empty in case of multiple results:

.. includecode:: code/Checks.scala#exists

.. _http-check-not-exists:

* ``notExists``

Checks that the value doesn't exist and or is empty in case of multiple results:

.. includecode:: code/Checks.scala#notExists

.. _http-check-in:

* ``in(sequence)``

Checks that the value belongs to a given sequence or vararg:

.. includecode:: code/Checks.scala#in

*sequence* is a function that returns a sequence of values of the same type of the previous step (extraction or transformation).

.. _http-check-optional:

* ``optional``

.. warning::
  ``optional`` used to be named ``dontValidate``. The old name has been deprecated, then removed in Gatling 2.1.

Always true, used for capture an optional value.

.. _http-check-validate:

* ``validate(validator)``

Built-ins validation steps actually resolve to this method.

*name* is the String that would be used to describe this part in case of a failure in the final error message.

*validator* is a ``Expression[Validator[X]]`` function that performs the validation logic.

.. includecode:: code/Checks.scala#validator

The ``apply`` method takes the actual extracted value and return a the Validation:
a Success containing the value to be passed to the next step, a Failure with the error message otherwise.

.. note:: In the case where no verifying step is defined, a ``exists`` is added implicitly.

.. _http-check-saving:

Saving
======

``saveAs(key)``

Saving is an **optional** step for storing the result of the previous step (extraction or transformation) into the virtual user Session, so that it can be reused later.

*key* is a ``String``.

.. _http-check-conditional:

Conditional Checking
====================

Check execution can be enslave to a condition.

``checkIf(condition)(thenCheck)``

The condition can be of two types:

* ``Expression[Boolean]``
* ``(Response, Session) => Validation[Boolean]``

Nested thenCheck will only be performed if condition is successful.

Putting it all together
=======================

To help you understand the checks, here is a list of examples:

.. includecode:: code/Checks.scala#regex-count-is

Verifies that there are exactly 5 HTTPS links in the response.

.. includecode:: code/Checks.scala#regex-findAll-is

Verifies that there are two secured links pointing at the specified websites.

.. includecode:: code/Checks.scala#status-is

Verifies that the status is equal to 200.

.. includecode:: code/Checks.scala#status-in

Verifies that the status is one of: 200, 201, 202, ..., 209, 210.

.. includecode:: code/Checks.scala#regex-find-exists

Verifies that there are at least **two** occurrences of "aWord".

.. includecode:: code/Checks.scala#regex-notExists

Verifies that the response doesn't contain "aWord".

.. includecode:: code/Checks.scala#bodyBytes-is-RawFileBody

Verifies that the response body matches the binary content of the file ``user-files/bodies/expected_response.json``

.. includecode:: code/Checks.scala#bodyString-isElFileBody

Verifies that the response body matches the text content of the file ``user-files/bodies/expected_template.json`` resolved with :ref:`Gatling Expression Language (EL) <el>`.
