---
title: "Checks"
description: "How to use generic checks available on most protocols supported in Gatling, such as regex, JsonPath, JMESPath or XPath to validate your response payloads and capture elements in there, so you can reuse them later."
lead: "Checks can be used to validate your request and extract elements which can be reused later"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 2030600
---

This page describes generic checks that can in theory be supported in all official protocols supported in Gatling.

Some protocols might implement specific checks, for example `status` for HTTP. Those are not described here, but in the documentation specific to this protocol.

## Concepts

Checks are used for 2 things:

* verifying that the response to a request matches some expectations
* capturing some elements in the response

Checks are typically attached to the parent object with the `check` method.
For example, on an HTTP request:

{{< include-code "status-is-200" java kt scala >}}

You can also define multiple checks at once:

{{< include-code "status-is-not-404-or-500" java kt scala >}}

This API provides a dedicated DSL for chaining the following steps:

1. [defining the check type]({{< ref "#check-type" >}})
2. [extracting]({{< ref "#extracting" >}})
3. [transforming]({{< ref "#transforming" >}})
4. [validating]({{< ref "#validating" >}})
5. [naming]({{< ref "#naming" >}})
6. [saving]({{< ref "#saving" >}})

## Generic Check Types {#check-type}

The following check types are generic and usually implemented on most official Gatling supported protocols.

#### `responseTimeInMillis`

Returns the response time of this request in milliseconds = the time between starting to send the request and finishing to receive the response.

{{< include-code "responseTimeInMillis" java kt scala >}}

#### `bodyString`

Return the full response body String.

{{< include-code "bodyString" java kt scala >}}

#### `bodyBytes`

Return the full response body byte array.

{{< include-code "bodyBytes" java kt scala >}}

#### `bodyLength`

Return the length of the response body in bytes (without the overhead of computing the bytes array).

{{< include-code "bodyLength" java kt scala >}}

#### `bodyStream`

Return an InputStream of the full response body bytes, typically to transform the received bytes before processing them.

{{< include-code "bodyStream" java kt scala >}}

#### `substring`

This check looks for the indices of the occurrences of a given substring inside the response body text.

It takes one single parameter:
* `pattern`  can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< alert tip >}}
Typically used for checking the presence of a substring, as it's more CPU efficient than a regular expression.
{{< /alert >}}

{{< include-code "substring" java kt scala >}}

#### `regex`

This check applies a [Java regular expression pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) on the response body text.

It takes one single parameter:
* `pattern`  can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< include-code "regex" java kt scala >}}

{{< alert tip >}}
In Java 15+ (Text blocks), Scala and Kotlin, you can use escaped strings with this notation: `"""my "non-escaped" string"""`.
This simplifies the writing and reading of regular expressions.
{{< /alert >}}

By default, it can extract 0 or 1 capture group, so the extracted type is `String`.
If your pattern contains more than one capture group, you must specify it with an extra step:

{{< include-code "regex-ofType" java kt scala >}}

#### `xpath`

This check applies an [XPath](https://en.wikipedia.org/wiki/XPath) query on an XML response body.

It takes two parameters:
* `expression`  can be a plain `String`, a Gatling Expression Language `String` or a function.
* `namespaces` is an optional List of couples of (prefix, uri). It's mandatory as soon as your document contains namespaces.

{{< include-code "xpath" java kt scala >}}

{{< alert tip >}}
XPath only works on well-formed XML documents, which HTML is not.

If you're looking for path based engine for parsing HTML documents, please have a look at our [CSS selectors support]({{< ref "#css" >}}).
{{< /alert >}}

#### `jsonPath`

[JsonPath](http://goessner.net/articles/JsonPath) is an XPath-like syntax for JSON.

{{< alert tip >}}
It lacks a proper specification, so behavior can differ depending on the implementation. Typically, some weird expressions supported in lax implementations in online evaluators might not work in Gatling.
You might want to consider using our [JMESPath support]({{< ref "#jmesPath" >}}) instead.
{{< /alert >}}

It takes one single parameter:
* `expression`  can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< include-code "jsonPath" java kt scala >}}

By default, it extracts Strings, meaning that non String values get serialized back into JSON.
You can tell Gatling the expected type with an extra step.
Note that the check will then fail if the actual value doesn't match the expected type.

{{< include-code "jsonPath-ofType" java kt scala >}}

For example, considering the following JSON payload:

```json
{
  "foo": 1,
  "bar": "baz"
}
```

... this is how you would extract an integer value:
{{< include-code "jsonPath-Int" java kt scala >}}

#### `jsonpJsonPath`

Same as [`jsonPath`]({{< ref "#jsonPath" >}}) but for [JSONP](http://en.wikipedia.org/wiki/JSONP).

#### `jmesPath`

[JMESPath](http://jmespath.org/) is a query language for JSON.

{{< alert tip >}}
It has the great advantage over [`jsonPath`]({{< ref "#jsonPath" >}}) that it comes with a full grammar and a Test Compliance Kit to validate implementations.
As a result, an expression tested with the online evaluator is guaranteed to work in Gatling too.
{{< /alert >}}

It takes one single parameter:
* *expression*  can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< include-code "jmesPath" java kt scala >}}

By default, it extracts Strings, meaning that non String values get serialized back into JSON.
You can tell Gatling the expected type with an extra step.
Note that the check will then fail is the actual value doesn't match the expected type.

{{< include-code "jmesPath-ofType" java kt scala >}}

For example, considering the following JSON payload:

```json
{
  "foo": 1,
  "bar": "baz"
}
```

... this is how you would extract an integer value:
{{< include-code "jmesPath-Int" java kt scala >}}

{{< alert tip >}}
You can use `registerJmesPathFunctions(io.burt.jmespath.function.Function*)` to register custom functions.
{{< /alert >}}

#### `jsonpJmesPath`

Same as [`jmesPath`]({{< ref "#jmesPath" >}}) but for [JSONP](http://en.wikipedia.org/wiki/JSONP).

#### `css`

This checks lets you apply [CSS Selectors](https://www.w3.org/TR/CSS21/selector.html%23id-selectors) on HTML response body text.

It takes two parameters:
* `selector` can be a plain `String`, a Gatling Expression Language `String` or a function.
* `attribute` is an optional static `String` if you want to target an attribute of the selected DOM nodes.

{{< include-code "css" java kt scala >}}

By default, it extracts Strings. In particular, if you haven't defined the `attribute` parameter, it will extract the node text content. You can force Gatling to actually capture the [jodd.lagarto.dom.Node](http://oblac.github.io/jodd-site/javadoc/jodd/lagarto/dom/Node.html) with an extra step:

{{< include-code "css-ofType" java kt scala >}}

#### `form`

This check uses a CSS Selector to capture all the defined or selected input parameters in a form tag into a Map.
Map values can be multivalued depending on if the input is multivalued or not
(input with `multiple` attribute set, or multiple occurrences of the same input name, except for radio).

It takes one single parameter:
* `selector` can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< include-code "form" java kt scala >}}

#### `md5` and `sha1` {#checksum}

This check computes a checksum of the response body. This can be useful to verify that a downloaded resource has not been corrupted in the process.

{{< include-code "checksum" java kt scala >}}

{{< alert tip >}}
Checksums are computed against the stream of chunks, so the whole body is not stored in memory.
{{< /alert >}}

## Extracting

The extraction step of the check DSL lets you filter the desired occurrence(s).

{{< alert tip >}}
If you don't explicitly define the extraction step, Gatling will perform an implicit [`find`]({{< ref "#find" >}}).
{{< /alert >}}


#### `find`

Filter one single element.
Target the first or only possible occurrence, depending on the check type.
If the check can capture multiple elements, `find` is identical to `find(0)`.

It comes in 2 flavors:
* parameterless, identical to `find(0)`
* with an integer parameter that is a 0 based rank of the occurrence. Only available on checks that can return multiple values.

{{< include-code "find" java kt scala >}}

#### `findAll`

Return all the occurrences. Only available on checks that can return multiple values.

{{< include-code "findAll" java kt scala >}}

#### `findRandom`

Return a random occurrence. Only available on checks that can return multiple values.

It comes in 2 flavors:
* parameterless, identical to `findRandom(1)`
* with a `num` int parameter and an optional `failIfLess` boolean parameter (default false, check will pick as many as possible) to extract several occurrences and optionally fail is the number of actual matches is less than the expected number.

{{< include-code "findRandom" java kt scala >}}

#### `count`

Returns the number of occurrences. Only available on checks that can return multiple values.

{{< include-code "count" java kt scala >}}

## Transforming

Transforming is an **optional** step that lets you transform the result of the extraction before trying to match it or save it.

#### `withDefault`

This optional step lets you provide a default value in case the previous step failed to capture anything.

It takes one single parameter:
* `defaultValue` can be a plain `String`, a Gatling Expression Language `String` or a function that must return the same type as the expected value

{{< include-code "withDefault" java kt scala >}}

#### `transform`

This step lets you pass a function that will only be triggered if the previous step was able to capture something.

It takes one single parameter:
* `function` is of type `X` to another possibly different type `X2`

{{< include-code "transform" java kt scala >}}

#### `transformWithSession`

This step is a variant of [`transform`]({{< ref "#transform" >}}) that lets you access the `Session` is order to compute the returned result.

It takes one single parameter:
* `function` is of type `(X, Session)` to another possibly different type `X2`

{{< include-code "transformWithSession" java kt scala >}}

#### `transformOption`

In contrary to [`transform`]({{< ref "#transform" >}}), this step is always invoked, even when the previous step failed to capture anything. 

{{< include-code "transformOption" java kt scala >}}

{{< alert tip >}}
If your goal is to provide a default value, [`withDefault`]({{< ref "#withdefault" >}}) is probably a more convenient way.
{{< /alert >}}

#### `transformOptionWithSession`

This step is a variant of [`transformOption`]({{< ref "#transformoption" >}}) that lets you access the `Session` is order to compute the returned result.

{{< include-code "transformOptionWithSession" java kt scala >}}

## Validating

{{< alert tip >}}
If you don't explicitly define the validation step, Gatling will perform an implicit [`exists`]({{< ref "#exists" >}}).
{{< /alert >}}

#### `is`

Validate that the value is equal to the expected one.

It takes one single parameter:
* `expected` can be a plain value whose type matches the extracted value, a Gatling Expression Language `String` or a function.

{{< include-code "is" java kt scala >}}

{{< alert tip >}}
`is` is a reserved keyword in Kotlin.
You can either protect it with backticks `` `is` `` or use the `shouldBe` alias instead.
{{< /alert >}}

#### `isNull`

Validate that the extracted value is null, typically a JSON value.

{{< include-code "isNull" java kt scala >}}

#### `not`

Validate that the extracted value is different from the expected one.

It takes one single parameter:
* `unexpected` can be a plain value whose type matches the extracted value, a Gatling Expression Language `String` or a function.

{{< include-code "not" java kt scala >}}

#### `notNull`

Validate that the extracted value is not null, typically a JSON value.

{{< include-code "notNull" java kt scala >}}

#### `exists`

Validate that the extracted value exists.

{{< include-code "exists" java kt scala >}}

#### `notExists`

Validate that the check didn't match and failed to extract anything.

{{< include-code "notExists" java kt scala >}}

#### `in`

Validate that the extracted value belongs to a given sequence or vararg.

{{< include-code "in" java kt scala >}}

{{< alert tip >}}
`in` is a reserved keyword in Kotlin.
You can either protect it with backticks `` `in` `` or use the `within` alias instead.
{{< /alert >}}

#### `optional`

Allows for the target to be missing. In this case, the check won't fail, the following
steps won't trigger, including saveAs, meaning possibly existing value won't be replaced nor removed.

#### `validate`

You can supply your own validator.

It takes two parameters:
* `name` is the String that would be used to describe this part in case of a failure in the final error message.
* `validator` is the validation logic function.

{{< include-code "validator" java kt scala >}}

## Naming

#### `name`

Naming is an **optional** step for customizing the name of the check in the error message in case of a check failure.

It takes one single parameter:
* `name` can only be a static String.

{{< include-code "name" java kt scala >}}

## Saving

#### `saveAs`

Saving is an **optional** step for storing the result of the check into the virtual user's Session, so that it can be reused later. It's only effective when the check is successful: it could match the response and passed validation.

It takes one single parameter:
* `key` can only be a static String.

{{< include-code "saveAs" java kt scala >}}

## Conditional Checking

#### `checkIf`

{{< alert tip >}}
Use `checkIf` instead of `check`.
{{< /alert >}}

Only perform the checks when some condition holds.

{{< include-code "checkIf" java kt scala >}}

## Putting It All Together

To help you understand the checks, here is a list of examples.

{{< include-code "all-together" java kt scala >}}
