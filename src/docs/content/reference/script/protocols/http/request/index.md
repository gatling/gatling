---
title: HTTP Request
seotitle: Gatling HTTP protocol reference - request
description: Create HTTP requests
lead: How to craft HTTP requests, including HTTP method like GET or POST, HTTP headers, query parameters, form parameters, body and checks.
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

{{< alert tip >}}
Note that most method can take static value, [Gatling Expression Language (EL)]({{< ref "../../core/session/el" >}}) strings and functions for parameters.
{{</ alert >}}

## Request Name

HTTP support has a dedicated DSL, whose entry point is the `http(requestName)` method.

This request name is important because it will act as a key when computing stats for the reports.
If the same name appears in multiple places in a Simulation, Gatling will consider the requests to be the same type, and their statistics will be aggregated.

{{< include-code "requestName" >}}

{{< alert warning >}}
As you can see, request names can be dynamic.
However, we recommend that you don't abuse this feature and end up with a very high number of distinct values.
You would put a huge burden on the reporting module and your stats might be harder to analyze and probably not meaningful (not enough values per request name).
{{</ alert >}}

HTTP requests must be passed to the `exec()` method to be attached to the scenario and executed.

{{< include-code "inline" >}}

## Method and URL

Gatling provides built-ins for the most common methods. Those are simply the method name in minor case.

{{< include-code "methods" >}}

{{< alert tip >}}
Gatling also supports relative URLs; see [baseUrl]({{< ref "protocol#baseurl" >}}).
{{< /alert >}}

## Query Parameters

Frameworks and developers often pass additional information in the query, which is the part of the URL after the `?`. A query is composed of *key=value* pairs, separated by `&`. Those are named *query parameters*.

For example, `https://github.com/gatling/gatling/issues?milestone=1&state=open` contains 2 query parameters:
* `milestone=1` : the key is *milestone* and its value is *1*
* `state=open` : the key is *state* and its value is *open*

{{< alert tip >}}
Query parameter keys and values have to be [URL encoded](http://www.w3schools.com/tags/ref_urlencode.asp), as per `RFC3986](http://tools.ietf.org/html/rfc3986).
By default, Gatling will try to take care of this encoding, but you can disable it; see [disableUrlEncoding]({{< ref "protocol#disableurlencoding" >}}).
{{< /alert >}}

To set the query parameters of an HTTP request, you can:

* either pass the full query in the URL, e.g.:

{{< include-code "full-query-in-url" >}}

* or pass query parameters one by one to the method named `queryParam`:

{{< include-code "queryParam" >}}

You can use `multivaluedQueryParam` to set query parameters with multiple values:

{{< include-code "multivaluedQueryParam" >}}

You can use `queryParamSeq` and `queryParamMap` to set multiple query parameters at once:

{{< include-code "queryParam-multiple" >}}

## Headers

#### `header`

The HTTP protocol uses headers to exchange information between the client and server that is not part of the message body.

Gatling HTTP allows you to specify any header you want with the `header` and `headers` methods.

{{< include-code "headers" >}}

{{< alert tip >}}
Header keys are defined as constants usable in the scenario, for example: `HttpHeaderNames.ContentType`.
You can find a list of the predefined constants [here](https://github.com/gatling/gatling/blob/main/gatling-http/src/main/scala/io/gatling/http/Headers.scala).
{{< /alert >}}

#### `asXXX`

Gatling provides some handy shortcuts for setting the required headers for JSON, XML, and form encodings: 

{{< include-code "asXXX" >}}

{{< alert tip >}}
Shared headers can also be defined on the [`HttpProtocol`]({{< ref "protocol#header" >}}).
{{< /alert >}}

For a given request, you can also disable shared `HttpProtocol` headers with `ignoreProtocolHeaders`.

{{< include-code "ignoreProtocolHeaders" >}}

## Checks

You can add checks on a request.

{{< include-code "check" >}}

For more information, see the [HTTP Checks reference section]({{< ref "checks" >}}).

For a given request, you can also disable common checks that were defined on the `HttpProtocol` with `ignoreProtocolChecks`:

{{< include-code "ignoreProtocolChecks" >}}

## Request Body

### Full Body

In this section, you can find the various methods for setting the full request body.

{{< alert tip >}}
Body templates location are resolved the same way as for [Feeder files]({{< ref "../../core/session/feeders#file-based-feeders" >}}).

Files must be placed in `src/main/resources` or `src/test/resources` when using a Maven (including the Gatling bundle), Gradle, or sbt project.
{{</ alert >}}

{{< alert warning >}}
Don't use relative filesystem paths such as ~~`src/main/resources/data/foo.txt`~~, instead use a classpath path `data/foo.txt`.
{{< /alert >}}

You can add a full body to an HTTP request with the dedicated method `body`, where body can be:

#### `StringBody`

`StringBody` lets you pass a text payload defined in your code.
The charset used for writing the bytes on the wire is the one defined in the `charset` attribute of the `Content-Type` request header if defined; otherwise the one defined in `gatling.conf` is used.

This solution is typically used with Strings in the [Gatling Expression Language]({{< ref "../../core/session/el" >}}) format.

It takes one single parameter:
* `string` the text content can be a plain `String`, a Gatling Expression Language `String`, or a function.

{{< include-code "StringBody" >}}

Using a function is one way to craft complex dynamic payloads, as you can code your own logic.

{{< include-code "template,template-usage" >}}

#### `RawFileBody` 

`RawFileBody` lets you pass a raw file whose bytes will be sent as is, meaning it can be binary content.
This way is the most efficient one as bytes can be cached and don't have to be decoded into text and then re-encoded back into bytes to be written on the wire. 

It takes one single parameter:
* `filePath` the file location, can be a plain `String`, a Gatling Expression Language `String`, or a function.

{{< include-code "RawFileBody" >}}

#### `ElFileBody` 

`ElFileBody` lets you pass some text content resolved from a template file in the [Gatling Expression Language]({{< ref "../../core/session/el" >}}) format.

It takes one single parameter:
* `filePath` the file location, can be a plain `String`, a Gatling Expression Language `String`, or a function.

Since Gatling EL is a text-based templating engine, content can not be non-textual.

{{< include-code "ElFileBody" >}}

#### `PebbleStringBody`

Gatling Expression Language is definitively the most optimized templating engine for Gatling in terms of raw performance. However, it's a bit limited in terms of logic you can implement in there.
If you want loops and conditional blocks, you can use Gatling's [Pebble](https://github.com/PebbleTemplates/pebble) based templating engine.

{{< include-code "PebbleStringBody" >}}

{{< alert tip >}}
You can register Pebble `Extensions` with `registerPebbleExtensions(extensions: Extension*)`.
This can only be done once and must be done before loading any Pebble template.
{{< /alert >}}

{{< alert warning >}}
The `registerPebbleExtensions` function is not supported by Gatling JS.
{{< /alert >}}

{{< alert tip >}}
Template inheritance is only available when using [`PebbleFileBody`]({{< ref "#pebblefilebody" >}}).
{{< /alert >}}

#### `PebbleFileBody`

`PebbleFileBody` lets you pass the path to a [Pebble](https://github.com/PebbleTemplates/pebble) file template.

{{< include-code "PebbleFileBody" >}}

#### `ByteArrayBody`

`ByteArrayBody` lets you pass an array of bytes, typically when you want to use a binary protocol such as Protobuf.

{{< include-code "ByteArrayBody" >}}

#### `InputStreamBody`

`InputStreamBody` lets you pass a `java.util.InputStream`.

{{< include-code "InputStreamBody" >}}

### Forms

This section refers to payloads encoded with [`application/x-www-form-urlencoded` or `multipart/form-data`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST), used with HTML forms.

{{< alert tip >}}
Unless you've explicitly set the `Content-Type` header:
* if you've set at least one file part, Gatling will set it to `multipart/form-data`
* otherwise, it will set it to `application/x-www-form-urlencoded`.
{{</ alert >}}

#### `formParam`

`formParam` lets you pass non-file form input fields.

{{< include-code "formParam" >}}

You can use `multivaluedFormParam` to set form parameters with multiple values:

{{< include-code "multivaluedFormParam" >}}

You can use `formParamSeq` and `formParamMap` to set multiple form parameters at once:

{{< include-code "formParam-multiple" >}}

#### `form`

You might want to repost all the inputs or a form previously captured with a [`form` check]({{< ref "../../core/checks#form" >}}).

Note you can override the form field values with the `formParam` and the likes.

{{< include-code "formFull" >}}

#### `formUpload`

This method takes 2 parameters:
* *name* the name of the form input can be a plain `String`, a Gatling Expression Language `String`, or a function.
* *filePath*, the path to the file, can be a plain `String`, a Gatling Expression Language `String`, or a function.

[See above]({{< ref "#full-body" >}}) how Gatling resolves `filePath`.

{{< include-code "formUpload" >}}

{{< alert tip >}}
The MIME Type of the uploaded file defaults to `application/octet-stream`, and the character set defaults to the one configured in `gatling.conf` (`UTF-8` by default). Override them when needed.
{{< /alert >}}

### Multipart

#### `bodyPart`

You can set a multipart body as individual parts using `bodyPart`.

{{< include-code "bodyPart" >}}

{{< alert tip >}}
The [asXXX shortcuts]({{< ref "#asxxx" >}}) can help you configure the necessary HTTP headers.
{{< /alert >}}


Once bootstrapped with one of the following methods, `BodyPart` has the following methods for setting additional options.
Like in the rest of the DSL, almost every parameter can be a plain `String`, a Gatling Expression Language `String`, or a function.

{{< include-code "bodyPart-options" >}}

#### `StringBodyPart`

Similar to [StringBody]({{< ref "#stringbody" >}}).

#### `RawFileBodyPart`

where path is the location of a file that will be uploaded as is.

Similar to [RawFileBody]({{< ref "#rawfilebody" >}}).

#### `ElFileBodyPart`

where path is the location of a file whose content will be parsed and resolved with the Gatling EL engine.

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

{{< include-code "processRequestBody" >}}

## Resources

Gatling provides a way to simulate a web browser fetching static.

A `resources` can be attached to a main request to define a list of HTTP requests to be executed once the main request completes.

Concurrency is defined by the HTTP version you're using:
* Over HTTP/1.1 (default), the number of concurrent requests per domain is capped by the maximum number of connections per domain, as defined by [`maxConnectionsPerHost`]({{< ref "protocol#maxconnectionsperhost" >}}). When the limit is reached, resources are fetched in the order they are passed to the `resources` block.
* Over HTTP/2, the number of concurrent requests per domain is uncapped and all resources are always fetched concurrently.

The next step in the scenario will only be executed once all the resources have completed.

{{< include-code "resources" >}}

## Advanced Options

#### `requestTimeout`

The default request timeout is controlled by the ``gatling.http.requestTimeout` configuration parameter.

However, you might want to use `requestTimeout`
to override the global value for a specific request, typically a long file upload or download.

{{< include-code "requestTimeout" >}}

#### `basicAuth` and `digestAuth` {#authorization}

Just like you can [define a global `basicAuth`  or `digestAuth` on the HttpProtocol configuration]({{< ref "protocol#authorization" >}}), you can define one on individual requests.

#### `proxy`

Just like you can [globally define a `proxy` on the HttpProtocol configuration]({{< ref "protocol#proxy" >}}), you can define one on individual requests.

#### `disableFollowRedirect`

Just like you can [globally disable following redirect on the HttpProtocol configuration]({{< ref "protocol#disablefollowredirect" >}}), you can define one on individual requests.

#### `disableUrlEncoding`

Just like you can [globally disable URL encoding on the HttpProtocol configuration]({{< ref "protocol#disableurlencoding" >}}), you can define one on individual requests.

#### `silent`

See [silencing protocol section]({{< ref "protocol#silenturi" >}}) for more details.

You can then make the request *silent*:

{{< include-code "silent" >}}

You might also want to do the exact opposite, typically on a given resource while resources have been globally turned silent at the protocol level:

{{< include-code "notSilent" >}}

#### `sign`

Just like you can [define a global signing strategy on the HttpProtocol configuration]({{< ref "protocol#sign" >}}), you can define one on individual requests.

#### `transformResponse`

Similarly, one might want to process the response before it's passed to the checks pipeline:

```scala
transformResponse(transformer: (Response, Session) => Validation[Response])
```

The example below shows how to decode some Base64 encoded response body:

{{< include-code "resp-processors-imports,response-processors" >}}
