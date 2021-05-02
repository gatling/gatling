---
title: "Checks"
description: "Use HTTP checks to validate your requests and capture elements"
lead: "HTTP checks can be used to validate your request and extract elements which can be reused later"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Concepts

The Check API is used for 2 things:

* verifying that the response to a request matches expectations
* eventually capturing some elements in it.

Checks are performed on a request with the `check` method.
For example, on an HTTP request:

{{< include-code "CheckSample.scala#status-is-200" scala >}}

One can of course perform multiple checks:

{{< include-code "CheckSample.scala#status-is-not-404-or-500" scala >}}

This API provides a dedicated DSL for chaining the following steps:

1. [defining the check type]({{< ref "#defining-check-type" >}})
2. [extracting]({{< ref "#extracting" >}})
3. [transforming]({{< ref "#transforming" >}})
4. [validating]({{< ref "#validating" >}})
5. [saving]({{< ref "#saving" >}})

{{< alert tip >}}
By default, Gatling follows redirects (can be disabled in the [protocol]({{< ref "../protocol#follow-redirects" >}})).
If this behavior is enabled, checks will ignore intermediate responses and will target the landing response.
{{< /alert >}}

## Defining the check type {#defining-check-type}

The HTTP Check implementation provides the following built-ins:

### HTTP status

* `status`

Targets the HTTP response status code.

{{< alert tip >}}
A status check is automatically added to a request when you don't specify one.
          It checks that the HTTP response has a 2XX or 304 status code.
{{< /alert >}}

### Page location

* `currentLocation`

Targets the current page absolute URL.
Useful when following redirects in order to check if the landing page is indeed the expected one.

* `currentLocationRegex(pattern)`

Same as above, but *pattern* is used to apply a regex on the current location.

By default, it can extract 0 or 1 capture group, so the extract type is `String`.

One can extract more than 1 capture group and define an different type with the `ofType[T]` extra step:

{{< include-code "CheckSample.scala#currentLocationRegex-ofType" scala >}}

Gatling provides built-in support for extracting String tuples from `Tuple2[String]` to `Tuple8[String]`.

The example below will capture two capture groups:

{{< include-code "CheckSample.scala#currentLocationRegex-example" scala >}}

### HTTP header

* `header(headerName)`

Targets the HTTP response header of the given name.
*headerName* can be a plain `String`, a `String` using Gatling EL or an `Expression[String]`.

* `headerRegex(headerName, pattern)`

Same as above, but *pattern* is used to apply a regex on the header value.

{{< alert tip >}}
The header names are available as constants in the DSL, accessible from the `HttpHeaderNames` object, e.g. `HttpHeaderNames.ContentType`.
{{< /alert >}}

By default, it can extract 0 or 1 capture group, so the extract type is `String`.

One can extract more than 1 capture group and define an different type with the `ofType[T]` extra step:

{{< include-code "CheckSample.scala#headerRegex-ofType" scala >}}

Gatling provides built-in support for extracting String tuples from `Tuple2[String]` to `Tuple8[String]`.

The example below will capture two capture groups:

{{< include-code "CheckSample.scala#headerRegex-example" scala >}}

### HTTP response body

HTTP checks are performed in the order of HTTP element precedence: first status, then headers, then response body.

Beware that, as an optimization, Gatling doesn't pile up response chunks unless a check is defined on the response body.

* `responseTimeInMillis`

Returns the response time of this request in milliseconds = the time between starting to send the request and finishing to receive the response.

* `bodyString`

Return the full response body String.
Note that this can be matched against content from the the filesystem using [RawFileBody]({{< ref "../request#request-body" >}}) or [ElFileBody]({{< ref "../request#request-body" >}}).

* `bodyBytes`

Return the full response body byte array.

* `bodyLength`

Return the length of the response body in bytes (without the overhead of computing the bytes array).

* `bodyStream`

Return an InputStream of the full response body bytes.

* `substring(expression)`

Scans for the indices of a given substring inside the body string.

*expression*  can be a plain `String`, a `String` using Gatling EL or an `Expression[String]`.

{{< include-code "CheckSample.scala#substring" scala >}}

{{< alert tip >}}
Typically used for checking the presence of a substring, as it's more CPU efficient than a regular expression.
{{< /alert >}}

* `regex(expression)`

Defines a Java regular expression to be applied on any text response body.

*expression*  can be a plain `String`, a `String` using Gatling EL or an `Expression[String]`.

It can contain multiple capture groups.

{{< include-code "CheckSample.scala#regex" scala >}}

{{< alert tip >}}
In Scala, you can use escaped strings with this notation: `"""my "non-escaped" string"""`.

This simplifies the writing and reading of regular expressions.
{{< /alert >}}

By default, it can extract 0 or 1 capture group, so the extract type is `String`.

You can extract more than 1 capture group and define an different type with the `ofType[T]` extra step:

{{< include-code "CheckSample.scala#regex-ofType" scala >}}

Gatling provides built-in support for extracting String tuples from `Tuple2[String]` to `Tuple8[String]`.

The example below will capture two capture groups:

{{< include-code "CheckSample.scala#regex-example" scala >}}

* `xpath(expression, namespaces)`

Defines an XPath 1.0 expression to be applied on an XML response body.

*expression*  can be a plain `String`, a `String` using Gatling EL or an `Expression[String]`.

*namespaces* is an optional List of couples of (prefix, uri)

{{< include-code "CheckSample.scala#xpath" scala >}}

{{< alert tip >}}
XPath only works on well formed XML documents, which regular HTML is not (while XHTML is).

If you're looking for path expression for matching HTML documents, please have a look at our :ref:`CSS selectors support<http-check-css>`.
{{< /alert >}}

* `jsonPath(expression)`

JsonPath is a XPath-like syntax for JSON. It was specified by Stefan Goessner.
Please check [Goessner's website](http://goessner.net/articles/JsonPath) for more information about the syntax.

*expression*  can be a plain `String`, a `String` using Gatling EL or an `Expression[String]`.

{{< include-code "CheckSample.scala#jsonPath" scala >}}

By default, it extracts `String`s, so JSON values of different types get serialized.

You can define an different type with the `ofType[T]` extra step:

{{< include-code "CheckSample.scala#jsonPath-ofType" scala >}}

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

{{< include-code "CheckSample.scala#json-response,jsonPath-Int" scala >}}

* `jsonpJsonPath(expression)`

Same as `jsonPath` but for [JSONP](http://en.wikipedia.org/wiki/JSONP).

* `jmesPath(expression)`

[JMESPath](http://jmespath.org/) is a query language for JSON.

*expression*  can be a plain `String`, a `String` using Gatling EL or an `Expression[String]`.

{{< include-code "CheckSample.scala#jmesPath" scala >}}

By default, it extracts `String`s, so JSON values of different types get serialized.

You can define an different type with the `ofType[T]` extra step:

{{< include-code "CheckSample.scala#jmesPath-ofType" scala >}}

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

{{< include-code "CheckSample.scala#json-response,jmesPath-Int" scala >}}

{{< alert tip >}}
You can use `registerJmesPathFunctions(io.burt.jmespath.function.Function*)` to register custom functions.
{{< /alert >}}

* `jsonpJmesPath(expression)`

Same as `jmesPath` but for [JSONP](http://en.wikipedia.org/wiki/JSONP).

* `css(expression, attribute)`

Gatling supports [CSS Selectors](https://lagarto.jodd.org/csselly/csselly).

*expression*  can be a plain `String`, a `String` using Gatling EL or an `Expression[String]`.

*attribute* is an optional `String`.

When filled, check is performed against the attribute value.
Otherwise check is performed against the node text content.

{{< include-code "CheckSample.scala#css" scala >}}

You can define an different return type with the `ofType[T]` extra step:

{{< include-code "CheckSample.scala#css-ofType" scala >}}

Gatling provides built-in support for the following types:

* String
* Node

Specifying a `Node` let you perform complex deep DOM tree traversing, typically in a `transform` check step.
Node is a [Jodd Lagarto](https://lagarto.jodd.org/lagarto-dom/lagartodom) [DOM Node](http://oblac.github.io/jodd-site/javadoc/jodd/lagarto/dom/Node.html).

* `form(expression)`

This check takes a CSS selector and returns a `Map[String, Any]` of the form field values.
Values are either of type `String` or `Seq[String]`, depending on if the input is multivalued or not
(input with `multiple` attribute set, or multiple occurrences of the same input name, except for radio).

* `md5` and `sha1`

Returns a checksum of the response body.
Checksums are computed efficiently against body parts as soon as they are received.
They are then discarded if not needed.

{{< alert tip >}}
checksums are computed against the stream of chunks, so the whole body is not stored in memory.
{{< /alert >}}

## Extracting

* `find`

Returns the first occurrence. If the check targets more than a single element, `find` is identical to `find(0)`.

{{< alert tip >}}
In the case where no extracting step is defined, a `find` is added implicitly.
{{< /alert >}}

### Multiple results

* `find(occurrence)`

Returns the occurrence of the given rank.

{{< alert tip >}}
Ranks start at 0.
{{< /alert >}}

* `findAll`

Returns a List of all the occurrences.

* `findRandom`

Returns a random match.

* `findRandom(num: Int)` and `findRandom(num: Int, failIfLess = true)`

Returns a given number of random matches, optionally failing is the number of actual matches is less than the expected number.

* `count`

Returns the number of occurrences.

`find(occurrence)`, `findAll`, `findRandom` and `count` are only available on check types that might produce multiple results.
For example, `status` only has `find`.

## Transforming

Transforming is an **optional** step for transforming the result of the extraction before trying to match or save it.

`transform(function)` takes a `X => X2` function, meaning that it can only transform the result when it exists.

{{< alert tip >}}
You can also gain read access to the `Session` with `transformWithSession` and pass a `(X, Session) => X2` instead.
{{< /alert >}}

`transformOption(function)` takes a `Option[X] => Validation[Option[X2]]` function, meaning that it gives full control over the extracted result, even providing a default value.

{{< alert tip >}}
You can also gain read access to the `Session` with `transformOptionWithSession` and pass a `(Option[X], Session) => Validation[X2]` instead.
{{< /alert >}}

{{< include-code "CheckSample.scala#transform,transformOption" scala >}}

## Validating

* `is(expected)`

Validate that the value is equal to the expected one, e.g.:

{{< include-code "CheckSample.scala#is" scala >}}

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a `String`, it can also be a `String` using Gatling EL or an `Expression[String]`.

* `isNull`

Validate that the extracted value is null, typically a JSON value, e.g.:

{{< include-code "CheckSample.scala#isNull" scala >}}

* `not(expected)`

Validate that the extracted value is different from the expected one:

{{< include-code "CheckSample.scala#not" scala >}}

*expected* is a function that returns a value of the same type of the previous step (extraction or transformation).

In case of a `String`, it can also be a `String` using Gatling EL or an `Expression[String]`.

* `notNull`

Validate that the extracted value is not null, typically a JSON value, e.g.:

{{< include-code "CheckSample.scala#notNull" scala >}}

* `exists`

Validate that the extracted value exists:

{{< include-code "CheckSample.scala#exists" scala >}}

* `notExists`

Validate that the check didn't match and couldn't extract anything:

{{< include-code "CheckSample.scala#notExists" scala >}}

* `in(sequence)`

Validate that the extracted value belongs to a given sequence or vararg:

{{< include-code "CheckSample.scala#in" scala >}}

*sequence* is a function that returns a sequence of values of the same type of the previous step (extraction or transformation).

* `optional`

Always true, used for capture an optional value.

* `validate(validator)`

Built-ins validation steps actually resolve to this method.

*name* is the String that would be used to describe this part in case of a failure in the final error message.

*validator* is a `Expression[Validator[X]]` function that performs the validation logic.

{{< include-code "CheckSample.scala#validator" scala >}}

The `apply` method takes the actual extracted value and return a the Validation:
a Success containing the value to be passed to the next step, a Failure with the error message otherwise.

{{< alert tip >}}
In the case where no verifying step is defined, a `exists` is added implicitly.
{{< /alert >}}

## Naming

`name(customName)`

Naming is an **optional** step for customizing the name of the check in the error message in case of a check failure.

## Saving

`saveAs(key)`

Saving is an **optional** step for storing the result of the previous step (extraction or transformation) into the virtual user Session, so that it can be reused later.

*key* is a `String`.

## Conditional Checking

Check execution can be enslave to a condition.

`checkIf(condition)(thenCheck)`

The condition can be of two types:

* `Expression[Boolean]`
* `(Response, Session) => Validation[Boolean]`

Nested thenCheck will only be performed if condition is successful.

## Putting it all together

To help you understand the checks, here is a list of examples:

{{< include-code "CheckSample.scala#regex-count-is" scala >}}

Verifies that there are exactly 5 HTTPS links in the response.

{{< include-code "CheckSample.scala#regex-findAll-is" scala >}}

Verifies that there are two secured links pointing at the specified websites.

{{< include-code "CheckSample.scala#status-is" scala >}}

Verifies that the status is equal to 200.

{{< include-code "CheckSample.scala#status-in" scala >}}

Verifies that the status is one of: 200, 201, 202, ..., 209, 210.

{{< include-code "CheckSample.scala#regex-find-exists" scala >}}

Verifies that there are at least **two** occurrences of "aWord".

{{< include-code "CheckSample.scala#regex-notExists" scala >}}

Verifies that the response doesn't contain "aWord".

{{< include-code "CheckSample.scala#bodyBytes-is-RawFileBody" scala >}}

Verifies that the response body matches the binary content of the file `user-files/bodies/expected_response.json`

{{< include-code "CheckSample.scala#bodyString-isElFileBody" scala >}}

Verifies that the response body matches the text content of the file `user-files/bodies/expected_template.json` resolved with [Gatling Expression Language (EL)]({{< ref "../../session/expression_el" >}}).
