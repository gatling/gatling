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

.. includecode:: code/CheckSample.scala#status-is-200

One can of course perform multiple checks:

.. includecode:: code/CheckSample.scala#status-is-not-404-or-500

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

.. includecode:: code/CheckSample.scala#currentLocationRegex-ofType

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups:

.. includecode:: code/CheckSample.scala#currentLocationRegex-example

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

.. includecode:: code/CheckSample.scala#headerRegex-ofType

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups:

.. includecode:: code/CheckSample.scala#headerRegex-example

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

* ``bodyLength``

Return the length of the response body in bytes (without the overhead of computing the bytes array).

.. _http-check-body-stream:

* ``bodyStream``

Return an InputStream of the full response body bytes.

.. _http-check-substring:

* ``substring(expression)``

Scans for the indices of a given substring inside the body string.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

.. includecode:: code/CheckSample.scala#substring

.. note:: Typically used for checking the presence of a substring, as it's more CPU efficient than a regular expression.

.. _http-check-regex:

* ``regex(expression)``

Defines a Java regular expression to be applied on any text response body.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

It can contain multiple capture groups.

.. includecode:: code/CheckSample.scala#regex

.. note:: In Scala, you can use escaped strings with this notation: ``"""my "non-escaped" string"""``.

          This simplifies the writing and reading of regular expressions.

By default, it can extract 0 or 1 capture group, so the extract type is ``String``.

You can extract more than 1 capture group and define an different type with the ``ofType[T]`` extra step:

.. includecode:: code/CheckSample.scala#regex-ofType

Gatling provides built-in support for extracting String tuples from ``Tuple2[String]`` to ``Tuple8[String]``.

The example below will capture two capture groups:

.. includecode:: code/CheckSample.scala#regex-example

.. _http-check-xpath:

* ``xpath(expression, namespaces)``

Defines an XPath 1.0 expression to be applied on an XML response body.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

*namespaces* is an optional List of couples of (prefix, uri)

.. includecode:: code/CheckSample.scala#xpath

.. note:: XPath only works on well formed XML documents, which regular HTML is not (while XHTML is).
          If you're looking for path expression for matching HTML documents, please have a look at our :ref:`CSS selectors support<http-check-css>`.

.. _http-check-jsonpath:

* ``jsonPath(expression)``

JsonPath is a XPath-like syntax for JSON. It was specified by Stefan Goessner.
Please check `Goessner's website <http://goessner.net/articles/JsonPath>`_ for more information about the syntax.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

.. includecode:: code/CheckSample.scala#jsonPath

By default, it extracts ``String``\ s, so JSON values of different types get serialized.

You can define an different type with the ``ofType[T]`` extra step:

.. includecode:: code/CheckSample.scala#jsonPath-ofType

Gatling provides built-in support for the following types:

* String (default): serializes back to valid JSON (meaning that special characters are escaped, e.g. `\n` and `\"`)
* Boolean
* Int
* Long
* Double
* Float
* Seq (JSON array)
* Map (JSON object)
* Any

The example below shows how to extract Ints:

.. includecode:: code/CheckSample.scala
  :include: json-response,jsonPath-Int

.. _http-check-jsonp-jsonpath:

* ``jsonpJsonPath(expression)``

Same as :ref:`jsonPath <http-check-jsonpath>` but for `JSONP <http://en.wikipedia.org/wiki/JSONP>`_.

.. _http-check-jmespath:

* ``jmesPath(expression)``

`JMESPath <http://jmespath.org/>`_ is a query language for JSON.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

.. includecode:: code/CheckSample.scala#jmesPath

By default, it extracts ``String``\ s, so JSON values of different types get serialized.

You can define an different type with the ``ofType[T]`` extra step:

.. includecode:: code/CheckSample.scala#jmesPath-ofType

Gatling provides built-in support for the following types:

* String (default): serializes back to valid JSON (meaning that special characters are escaped, e.g. `\n` and `\"`)
* Boolean
* Int
* Long
* Double
* Float
* Seq (JSON array)
* Map (JSON object)
* Any

The example below shows how to extract Ints:

.. includecode:: code/CheckSample.scala
  :include: json-response,jmesPath-Int

.. note:: You can use ``registerJmesPathFunctions(io.burt.jmespath.function.Function*)`` to register custom functions.

.. _http-check-jsonp-jsonpath:

* ``jsonpJmesPath(expression)``

Same as :ref:`jmesPath <http-check-jmespath>` but for `JSONP <http://en.wikipedia.org/wiki/JSONP>`_.

.. _http-check-css:

* ``css(expression, attribute)``

Gatling supports `CSS Selectors <https://jodd.org/csselly/>`_.

*expression*  can be a plain ``String``, a ``String`` using Gatling EL or an ``Expression[String]``.

*attribute* is an optional ``String``.

When filled, check is performed against the attribute value.
Otherwise check is performed against the node text content.

.. includecode:: code/CheckSample.scala#css

You can define an different return type with the ``ofType[T]`` extra step:

.. includecode:: code/CheckSample.scala#css-ofType

Gatling provides built-in support for the following types:

* String
* Node

Specifying a ``Node`` let you perform complex deep DOM tree traversing, typically in a ``transform`` check step.
Node is a `Jodd Lagardo <https://jodd.org/lagarto/>`_ DOM `Node <http://oblac.github.io/jodd-site/javadoc/jodd/lagarto/dom/Node.html>`_.

* ``form(expression)``

This check takes a CSS selector and returns a ``Map[String, Any]`` of the form field values.
Values are either of type ``String`` or `Seq[String]``, depending on if the input is multivalued or not
(input with ``multiple`` attribute set, or multiple occurrences of the same input name, except for radio).

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

.. _http-check-find-random:

* ``findRandom``

Returns a random match.

* ``findRandom(num: Int)`` and ``findRandom(num: Int, failIfLess = true)``

Returns a given number of random matches, optionally failing is the number of actual matches is less than the expected number.

.. _http-check-count:

* ``count``

Returns the number of occurrences.

``find(occurrence)``, ``findAll``, ``findRandom`` and ``count`` are only available on check types that might produce multiple results.
For example, ``status`` only has ``find``.

.. _http-check-transform:

Transforming
============

Transforming is an **optional** step for transforming the result of the extraction before trying to match or save it.

``transform(function)`` takes a ``X => X2`` function, meaning that it can only transform the result when it exists.

.. note:: You can also gain read access to the ``Session`` with `transformWithSession` and pass a ``(X, Session) => X2`` instead.

``transformOption(function)`` takes a ``Option[X] => Validation[Option[X2]]`` function, meaning that it gives full control over the extracted result, even providing a default value.

.. note:: You can also gain read access to the ``Session`` with `transformOptionWithSession` and pass a ``(Option[X], Session) => Validation[X2]`` instead.

.. includecode:: code/CheckSample.scala
   :include: transform,transformOption

.. _http-check-validating:

Validating
==========

.. _http-check-is:

* ``is(expected)``

Validate that the value is equal to the expected one, e.g.:

.. includecode:: code/CheckSample.scala#is

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a ``String``, it can also be a ``String`` using Gatling EL or an ``Expression[String]``.

.. _http-check-isNull:

* ``isNull``

Validate that the extracted value is null, typically a JSON value, e.g.:

.. includecode:: code/CheckSample.scala#isNull

.. _http-check-not:

* ``not(expected)``

Validate that the extracted value is different from the expected one:

.. includecode:: code/CheckSample.scala#not

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a ``String``, it can also be a ``String`` using Gatling EL or an ``Expression[String]``.

.. _http-check-notNull:

* ``notNull``

Validate that the extracted value is not null, typically a JSON value, e.g.:

.. includecode:: code/CheckSample.scala#notNull

.. _http-check-exists:

* ``exists``

Validate that the extracted value exists:

.. includecode:: code/CheckSample.scala#exists

.. _http-check-not-exists:

* ``notExists``

Validate that the check didn't match and couldn't extract anything:

.. includecode:: code/CheckSample.scala#notExists

.. _http-check-in:

* ``in(sequence)``

Validate that the extracted value belongs to a given sequence or vararg:

.. includecode:: code/CheckSample.scala#in

*sequence* is a function that returns a sequence of values of the same type of the previous step (extraction or transformation).

.. _http-check-optional:

* ``optional``

Always true, used for capture an optional value.

.. _http-check-validate:

* ``validate(validator)``

Built-ins validation steps actually resolve to this method.

*name* is the String that would be used to describe this part in case of a failure in the final error message.

*validator* is a ``Expression[Validator[X]]`` function that performs the validation logic.

.. includecode:: code/CheckSample.scala#validator

The ``apply`` method takes the actual extracted value and return a the Validation:
a Success containing the value to be passed to the next step, a Failure with the error message otherwise.

.. note:: In the case where no verifying step is defined, a ``exists`` is added implicitly.

.. _http-check-naming:

Naming
======

``name(customName)``

Naming is an **optional** step for customizing the name of the check in the error message in case of a check failure.

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

.. includecode:: code/CheckSample.scala#regex-count-is

Verifies that there are exactly 5 HTTPS links in the response.

.. includecode:: code/CheckSample.scala#regex-findAll-is

Verifies that there are two secured links pointing at the specified websites.

.. includecode:: code/CheckSample.scala#status-is

Verifies that the status is equal to 200.

.. includecode:: code/CheckSample.scala#status-in

Verifies that the status is one of: 200, 201, 202, ..., 209, 210.

.. includecode:: code/CheckSample.scala#regex-find-exists

Verifies that there are at least **two** occurrences of "aWord".

.. includecode:: code/CheckSample.scala#regex-notExists

Verifies that the response doesn't contain "aWord".

.. includecode:: code/CheckSample.scala#bodyBytes-is-RawFileBody

Verifies that the response body matches the binary content of the file ``user-files/bodies/expected_response.json``

.. includecode:: code/CheckSample.scala#bodyString-isElFileBody

Verifies that the response body matches the text content of the file ``user-files/bodies/expected_template.json`` resolved with :ref:`Gatling Expression Language (EL) <el>`.
