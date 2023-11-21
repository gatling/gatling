---
title: "HTTP Request"
description: "Create HTTP requests"
lead: "How to craft HTTP requests, including HTTP method like GET or POST, HTTP headers, query parameters, form parameters, body and checks."
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 2050100
---

{{< alert tip >}}
Note that most method can take static value, [Gatling Expression Language (EL)]({{< ref "../../core/session/el" >}}) strings and functions for parameters.
{{</ alert >}}

## Request Name

HTTP support has a dedicated DSL, whose entry point is the `http(requestName)` method.

This request name is important because it will act as a key when computing stats for the reports.
If the same name appears in multiple places in a Simulation, Gatling will consider those requests are of the same type and their statistics will be aggregated.

{{< include-code "requestName" java kt scala >}}

{{< alert warning >}}
As you can see, request names can be dynamic.
However, we recommend that you don't abuse this feature and end up with a very high number of distinct values.
You would put a huge burden on the reporting module and your stats might be harder to analyze and probably not meaningful (not enough values per request name).
{{</ alert >}}

HTTP requests have to be passed to the `exec()` method in order to be attached to the scenario and be executed.

{{< include-code "inline" java kt scala >}}

## Method and URL

Gatling provides built-ins for the most common methods. Those are simply the method name in minor case.

{{< include-code "methods" java kt scala >}}

{{< alert tip >}}
Gatling also support relative urls, see [baseUrl]({{< ref "../protocol#baseurl" >}}).
{{< /alert >}}

## Query Parameters

Frameworks and developers often pass additional information in the query, which is the part of the url after the `?`. A query is composed of *key=value* pairs, separated by `&`. Those are named *query parameters*.

For example, `https://github.com/gatling/gatling/issues?milestone=1&state=open` contains 2 query parameters:
* `milestone=1` : the key is *milestone* and its value is *1*
* `state=open` : the key is *state* and its value is *open*

{{< alert tip >}}
Query parameter keys and values have to be [URL encoded](http://www.w3schools.com/tags/ref_urlencode.asp), as per `RFC3986](http://tools.ietf.org/html/rfc3986).
By default, Gatling will try to take care of this encoding, but you can disable it, see [disableUrlEncoding]({{< ref "../protocol#disableurlencoding" >}}).
{{< /alert >}}

In order to set the query parameters of an HTTP request, you can:

* either pass the full query in the url, e.g.:

{{< include-code "full-query-in-url" java kt scala >}}

* or pass query parameters one by one to the method named `queryParam`:

{{< include-code "queryParam" java kt scala >}}

You can use `multivaluedQueryParam` to set query parameters with multiple values:

{{< include-code "multivaluedQueryParam" java kt scala >}}

You can use `queryParamSeq` and `queryParamMap` to set multiple query parameters at once:

{{< include-code "queryParam-multiple" java kt scala >}}

## Headers

#### `header`

The HTTP protocol uses headers to exchange information between client and server that is not part of the message body.

Gatling HTTP allows you to specify any header you want to with the `header` and `headers` methods.

{{< include-code "headers" java kt scala >}}

{{< alert tip >}}
Headers keys are defined as constants usable in the scenario, for example: `HttpHeaderNames.ContentType`.
You can find a list of the predefined constants [here](https://github.com/gatling/gatling/blob/main/gatling-http/src/main/scala/io/gatling/http/Headers.scala).
{{< /alert >}}

#### `asXXX`

Gatling provides some handy shortcuts for setting the required headers for JSON, XML and form encodings: 

{{< include-code "asXXX" java kt scala >}}

{{< alert tip >}}
Shared headers can also be defined on the [`HttpProtocol`]({{< ref "../protocol#header" >}}).
{{< /alert >}}

For a given request, you can also disable shared `HttpProtocol` headers with `ignoreProtocolHeaders`.

{{< include-code "ignoreProtocolHeaders" java kt scala >}}

## Checks

You can add checks on a request.

{{< include-code "check" java kt scala >}}

For more information, see the [HTTP Checks reference section]({{< ref "../check" >}}).

For a given request, you can also disable common checks that were defined on the `HttpProtocol` with `ignoreProtocolChecks`:

{{< include-code "ignoreProtocolChecks" java kt scala >}}

## Request Body

### Full Body

In this section, you can find the various methods for setting the full request body.

{{< alert tip >}}
Body templates location are resolved the same way as for [Feeder files]({{< ref "../../core/session/feeder/#file-based-feeders" >}}).

Files must be placed in:
* the `user-files/resources` directory when using the bundle distribution
* `src/main/resources` or `src/test/resources` when using a maven, gradle or sbt project
{{</ alert >}}

{{< alert warning >}}
Don't use relative filesystem paths such as ~~`src/main/resources/data/foo.txt`~~, instead use a classpath path `data/foo.txt`.
{{< /alert >}}

You can add a full body to an HTTP request with the dedicated method `body`, where body can be:

#### `StringBody`

`StringBody` lets you pass a text payload defined in your code.
The charset used writing the bytes on the wire is the one defined in the `charset` attribute of the `Content-Type` request header if defined, otherwise the one defined in `gatling.conf`.

This solution is typically used with Strings in the [Gatling Expression Language]({{< ref "../../core/session/el" >}}) format.

It takes one single parameter:
* `string` the text content, can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< include-code "StringBody" java kt scala >}}

Using function is one way to craft complex dynamic payload as you can code your own logic.

{{< include-code "template,template-usage" java kt scala >}}

#### `RawFileBody` 

`RawFileBody` lets you pass a raw file whose bytes will be sent as is, meaning it can be binary content.
This way is the most efficient one as bytes can be cached and don't have to be decoded into text and then re-encoded back into bytes to be written on the wire. 

It takes one single parameter:
* `filePath` the file location, can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< include-code "RawFileBody" java kt scala >}}

#### `ElFileBody` 

`ElFileBody` lets you pass some text content resolved from a template file in the [Gatling Expression Language]({{< ref "../../core/session/el" >}}) format.

It takes one single parameter:
* `filePath` the file location, can be a plain `String`, a Gatling Expression Language `String` or a function.

As Gatling EL is a text based templating engine, content can not be non-textual.

{{< include-code "ElFileBody" java kt scala >}}

#### `PebbleStringBody`

Gatling Expression Language is definitively the most optimized templating engine for Gatling, in terms of raw performance. However, it's a bit limited in terms of logic you can implement in there.
If you want loops and conditional blocks, you can use Gatling's [Pebble](https://github.com/PebbleTemplates/pebble) based templating engine.

{{< include-code "PebbleStringBody" java kt scala >}}

{{< alert tip >}}
You can register Pebble `Extensions`s with `registerPebbleExtensions(extensions: Extension*)`. This can only be done once, and must be done prior to loading any Pebble template.
{{< /alert >}}

{{< alert tip >}}
Template inheritance is only available when using [`PebbleFileBody`]({{< ref "#pebblefilebody" >}}).
{{< /alert >}}

#### `PebbleFileBody`

`PebbleFileBody` lets you pass the path to a [Pebble](https://github.com/PebbleTemplates/pebble) file template.

{{< include-code "PebbleFileBody" java kt scala >}}

#### `ByteArrayBody`

`ByteArrayBody` lets you pass an array of bytes, typically when you want to use a binary protocol such as Protobuf.

{{< include-code "ByteArrayBody" java kt scala >}}

#### `InputStreamBody`

`InputStreamBody` lets you pass an `java.util.InputStream`.

{{< include-code "InputStreamBody" java kt scala >}}

### Forms

This section refers to payloads encoded with [`application/x-www-form-urlencoded` or `multipart/form-data`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST), used with HTML forms.

{{< alert tip >}}
Unless you've explicitly set the `Content-Type` header:
* if you've set at least one file part, Gatling will set it to `multipart/form-data`
* otherwise, it will set it to `application/x-www-form-urlencoded`.
{{</ alert >}}

#### `formParam`

`formParam` lets you pass non file form input fields.

{{< include-code "formParam" java kt scala >}}

You can use `multivaluedFormParam` to set form parameters with multiple values:

{{< include-code "multivaluedFormParam" java kt scala >}}

You can use `formParamSeq` and `formParamMap` to set multiple form parameters at once:

{{< include-code "formParam-multiple" java kt scala >}}

#### `form`

You might want to repost all the inputs or a form previously captured with a [`form` check]({{< ref "../../core/check#form" >}}).

Note you can override the form field values with the `formParam` and the likes.

{{< include-code "formFull" java kt scala >}}

#### `formUpload`

This method takes 2 parameters:
* *name* the name of the form input, can be a plain `String`, a Gatling Expression Language `String` or a function.
* *filePath* the path to the file, can be a plain `String`, a Gatling Expression Language `String` or a function.

[See above]({{< ref "#full-body" >}}) how Gatling resolves `filePath`.

{{< include-code "formUpload" java kt scala >}}

{{< alert tip >}}
The MIME Type of the uploaded file defaults to `application/octet-stream` and the character set defaults to the one configured in `gatling.conf` (`UTF-8` by default). Override them when needed.
{{< /alert >}}

### Multipart

#### `bodyPart`

You can set a multipart body as individual parts using `bodyPart`.

{{< include-code "bodyPart" java kt scala >}}

{{< alert tip >}}
The [asXXX shortcuts]({{< ref "#asxxx" >}}) can help you configure the necessary HTTP headers.
{{< /alert >}}


Once bootstrapped with one of the following methods, `BodyPart` has the following methods for setting additional options.
Like in the rest of the DSL, almost every parameter can be a plain `String`, a Gatling Expression Language `String` or a function.

{{< include-code "bodyPart-options" java kt scala >}}

#### `StringBodyPart`

Similar to [StringBody]({{< ref "#stringbody" >}}).

#### `RawFileBodyPart`

where path is the location of a file that will be uploaded as is.

Similar to [RawFileBody]({{< ref "#rawfilebody" >}}).

#### `ElFileBodyPart`

where path is the location of a file whose content will be parsed and resolved with Gatling EL engine.

Similar to [ElFileBody]({{< ref "#elfilebody" >}}).

#### `PebbleStringBodyPart`

Similar to [PebbleStringBody]({{< ref "#pebblestringbody" >}}).

#### `PebbleFileBodyPart`

Similar to [PebbleFileBody]({{< ref "#pebblefilebody" >}}).

#### `ByteArrayBodyPart`

Similar to [ByteArrayBody]({{< ref "#bytearraybody" >}}).

### Pre-Processing

#### `processRequestBody`

You might want to process the request body before it's being sent to the wire.
Gatling currently only provides one single pre-processor: `gzipBody`.

{{< include-code "processRequestBody" java kt scala >}}

## Resources

Gatling can fetch a main request's resources in parallel in order to emulate the behavior of a real web browser.

{{< include-code "resources" java kt scala >}}

## Advanced Options

#### `requestTimeout`

The default request timeout is controlled by the ``gatling.http.requestTimeout` configuration parameter.

However, you might want to use `requestTimeout`
to override the global value for a specific request, typically a long file upload or download.

{{< include-code "requestTimeout" java kt scala >}}

#### `basicAuth` and `digestAuth` {#authorization}

Just like you can [define a global `basicAuth`  or `digestAuth` on the HttpProtocol configuration]({{< ref "../protocol#authorization" >}}), you can define one on individual requests.

#### `proxy`

Just like you can [globally define a `proxy` on the HttpProtocol configuration]({{< ref "../protocol#proxy" >}}), you can define one on individual requests.

#### `virtualHost`

Just like you can [globally define a `virtualHost` on the HttpProtocol configuration]({{< ref "../protocol#virtualhost" >}}), you can define one on individual requests.

#### `disableFollowRedirect`

Just like you can [globally disable following redirect on the HttpProtocol configuration]({{< ref "../protocol#disablefollowredirect" >}}), you can define one on individual requests.

#### `disableUrlEncoding`

Just like you can [globally disable URL encoding on the HttpProtocol configuration]({{< ref "../protocol#disableurlencoding" >}}), you can define one on individual requests.

#### `silent`

See [silencing protocol section]({{< ref "../protocol#silenturi" >}}) for more details.

You can then make the request *silent*:

{{< include-code "silent" java kt scala >}}

You might also want to do the exact opposite, typically on a given resource while resources have been globally turned silent at protocol level:

{{< include-code "notSilent" java kt scala >}}

#### `sign`

Just like you can [define a global signing strategy on the HttpProtocol configuration]({{< ref "../protocol#sign" >}}), you can define one on individual requests.

#### `transformResponse`

Similarly, one might want to process the response before it's passed to the checks pipeline:

`transformResponse(responseTransformer: (Response, Session) => Validation[Response])`

The example below shows how to decode some Base64 encoded response body:

{{< include-code "resp-processors-imports,response-processors" java kt scala >}}
