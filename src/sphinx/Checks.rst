******
Checks
******

Concepts
--------

The Check API is used for verifying that the response to a request
matches expectations and capturing some elements in it.

Checks are performed on a request thanks to the method ``check``. For
example, on an HTTP request :

.. code:: scala

    http("My Request").get("myUrl").check(status.is(200))

One can of course perform multiple checks:

.. code:: scala

    http("My Request").get("myUrl").check(status.not(404), status.not(500)))

This API provides a dedicated DSL for chaining the following steps:

1. defining the check
2. extracting
3. transforming
4. verifying
5. saving

Defining the check type
-----------------------

This part is specific to a given Check implementation.

The HTTP Check implementation provides the following built-ins:

HTTP status:
~~~~~~~~~~~~

-  ``status``

Targets the HTTP response status code.

    Note: A status check is automatically added to a request when you
    don't specify one. It checks that the HTTP response has a 2XX status
    code.

-  ``currentLocation`` *(since 1.3.0)*

Targets the current page absolute URL. Useful when following redirects
in order to check if the landing page is indeed the expected one.

HTTP header:
~~~~~~~~~~~~

-  ``header(headerName)``

Targets the HTTP response header of the given name. *headerName* can be
a simple String, an evaluatable String containing expression, or a
(Session => String) function.

-  ``headerRegex(headerName, pattern)``

Same than above, but *pattern* is used to apply a regex on the header
value.

    Note: The header names are available as constants in the DSL. They
    all are written in upper case and words are separated with
    underscores, eg: CONTENT\_TYPE

    Note: ``Location`` header value is automatically decoded when
    performing a check on it

HTTP response body:
~~~~~~~~~~~~~~~~~~~

HTTP checks are performed in the order of HTTP element precedence: first
status, then headers, then response body.

Beware that, as an optimization, Gatling doesn't pile up response chunks
unless a check is defined on the response body.

-  ``responseTimeInMillis`` *(since 1.2.4)*

Returns the response time of this request in milliseconds = the time
between starting to send the request and finishing to receive the
response.

-  ``latencyInMillis`` *(since 1.2.4)*

Returns the latency of this request in milliseconds = the time between
finishing to send the request and starting to receive the response.

-  ``bodyString`` *(since 1.3.1)*

Return the full response body.

-  ``regex(expression)``

Defines a Java regular expression to be applied on any text response
body.

*expression* can be a simple String, an evaluatable String containing
expression, or a (Session => String) function.

It can contain 0 or 1 capture group.

.. code:: scala

    regex("""<td class="number">""")
    regex("""<td class="number">ACC${account_id}</td>""")
    regex("""/private/bank/account/(ACC[0-9]*)/operations.html""")

    Note: In scala, you can use escaped strings with this notation:
    ``"""my "non-escaped" string"""``. This simplifies the writing and
    reading of regular expressions.

-  ``xpath(expression, namespaces)``

Defines an XPath 1.0 expression to be applied on an XML response body.

*expression* can be a simple String, an evaluatable String containing
expression, or a (Session => String) function.

*namespaces* is an optional List of couples of (prefix, uri)

.. code:: scala

    xpath("//input[@id='text1']/@value")
    xpath("//foo:input[@id='text1']/@value", List("foo" -> "http://foo.com"))

    Note: You can also use vtdXpath(xpathExpression: String), this check
    uses VTD as the XPath engine, it is available as a `separate module
    <https://github.com/excilys/gatling-vtd>`__.

-  ``jsonPath(expression)``

Based on `Goessner's JsonPath <http://goessner.net/articles/JsonPath>`__.

*expression* can be a simple String, a String containing an EL
expression, or a (Session => String) function.

.. code:: scala

    jsonPath("$..foo.bar[2].baz")

-  ``css(expression, attribute)`` *(since 1.2.0)*

Gatling supports `CSS Selectors <http://jodd.org/doc/csselly>`__.

*expression* can be a simple String, a String containing an EL
expression, or a (Session => String) function.

*attribute* is an optional String. When filled, check is performed
against the attribute value. Otherwise check is performed against the
node text content.

-  ``md5`` and ``sha1`` *(since 1.2.2)*

Returns a checksum of the response body. Checksums are computed
efficiently against body parts as soon as there's received. Those are
then discarded if not needed.

    Note: checksums are computed against the stream of chunks, so the
    whole body is not stored in memory.

Extracting
----------

-  ``find``: return the first occurrence

-  ``find(occurrence)``: return the occurrence of the given rank

    Note: Ranks start at 0.

-  ``findAll``: return a List of all the occurrences

-  ``count``: return the number of occurrences

find(occurrence), findAll and count are only available on check types
that might produce multiple results. For example, status only has find.

    Note: In case of no extracting step is defined, a *find* is added
    implicitly.

Transforming
------------

'transform(transformationFunction)' Transforming is an **optional** step
for transforming the result of the extraction before trying to match or
save it.

*transformationFunction* is a function whose input is the extraction
result and output is the result of your transformation.

.. code:: scala

    transform(string => string + "foo")'

Verifying
---------

-  ``is(expected)``

Checks that the value is equal to the expected one.

*expected* is a function that returns a value of the same type of the
previous step (extraction or transformation). In case of a String, it
can also be a static String or a String with an EL expression.

-  ``not(expected)``

Checks that the value is different from the expected one.

*expected* is a function that returns a value of the same type of the
previous step (extraction or transformation). In case of a String, it
can also be a static String or a String with an EL expression.

-  ``exists``

Checks that the value exists and is not empty in case of multiple
results.

-  ``notExists``

Checks that the value doesn't exist and or is empty in case of multiple
results.

-  ``in(sequence)``

Checks that the value belongs to a given sequence.

-  ``whatever``

Always true, used for capture an optional value.

*expected* is a function that returns a sequence of values of the same
type of the previous step (extraction or transformation).

    Note: In case of no verifying step is defined, a *exists* is added
    implicitly.

Saving
------

``saveAs(key)``

Saving is an optional step for storing the result of the previous step
(extraction or transformation) into the virtual user Session, so that it
can be reused later.

*key* is a String

Putting it all together
-----------------------

To help you understand the checks, here is a list of examples:

.. code:: scala

    check(regex("""https://(.*)""").count.is(5))

Verifies that there are exactly 5 HTTPS links in the response

.. code:: scala

    check(regex("""https://(.*)/.*""")
          .findAll
          .is(List("www.google.com", "www.mysecuredsite.com"))

Verifies that there are two secured links pointing at the specified
websites.

.. code:: scala

    check(status.is(200))

Verifies that the status is equal to 200

.. code:: scala

    check(status.in(200 to 210))

Verifies that the status is one of: 200, 201, 202, ..., 209, 210

.. code:: scala

    check(regex("aWord").find(1).exists))

Verifies that there are at least **two** occurrences of "aWord"

.. code:: scala

    check(regex("aWord").notExists)

Verifies that the response doesn't contain "aWord"
