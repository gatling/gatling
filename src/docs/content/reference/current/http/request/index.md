---
title: "HTTP Request"
description: "Create HTTP requests"
lead: "Learn about get put post delete head patch options method, and configure the query parameters, HTTP checks, multipart and request body"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

HTTP support has a dedicated DSL, whose entry point is the `http(requestName: Expression[String])` method.

This request name is important because it will act as a key when computing stats for the reports.
If the same name appears in multiple places in a Simulation, Gatling will consider those requests are of the same type and their statistics will be aggregated.

HTTP requests have to be passed to the `exec()` method in order to be attached to the scenario and be executed.

{{< include-code "HttpRequestSample.scala#example-embedded-or-not" scala >}}

## Method and URL

HTTP protocol requires 2 mandatory parameters: the method and the URL.

Gatling provides built-ins for the most common methods. Those are simply the method name in minor case:

* `get(url: Expression[String])`
* `put(url: Expression[String])`
* `post(url: Expression[String])`
* `delete(url: Expression[String])`
* `head(url: Expression[String])`
* `patch(url: Expression[String])`
* `options(url: Expression[String])`

{{< alert tip >}}
These methods are the ones used in REST web services and RESTful applications; thus, such services can be tested with Gatling.
{{< /alert >}}

Gatling also supports custom methods (e.g. you can use the method *PURGE* to purge Nginx cache):

* `httpRequest(method: String, url: Expression[String])`

This is how an HTTP request is declared:

{{< include-code "HttpRequestSample.scala#general-structure,builtins-or-custom" scala >}}

## Query Parameters

Frameworks and developers often pass additional information in the query, which is the part of the url after the `?`. A query is composed of *key=value* pairs, separated by `&`. Those are named *query parameters*.

For example, `https://github.com/gatling/gatling/issues?milestone=1&state=open` contains 2 query parameters:

* `milestone=1` : the key is *milestone* and its value is *1*
* `state=open` : the key is *state* and its value is *open*

{{< alert tip >}}
Query parameter keys and values have to be [URL encoded](http://www.w3schools.com/tags/ref_urlencode.asp), as per `RFC3986](http://tools.ietf.org/html/rfc3986).
Sometimes, HTTP server implementations are very permissive, but Gatling currently isn't and sticks to the RFC.
{{< /alert >}}

In order to set the query parameters of an HTTP request, you can:

* either pass the full query in the url, e.g.:

{{< include-code "HttpRequestSample.scala#getting-issues" scala >}}

* or pass query parameters one by one to the method named `queryParam(key: Expression[String], value: Expression[Any])`, e.g.:

{{< include-code "HttpRequestSample.scala#query-params-no-el" scala >}}

Of course, you can use [Gatling Expression Language (EL)]({{< ref "../../session/expression_el" >}}) to make those values dynamic based on data in the virtual user's session:

{{< include-code "HttpRequestSample.scala#query-params-with-el" scala >}}

If you'd like to specify a query parameter without value, you have to use `queryParam("key", "")`:

{{< include-code "HttpRequestSample.scala#query-param-no-value" scala >}}

If you'd like to pass multiple values for your parameter, but all at once, you can use `multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]])`:

{{< include-code "HttpRequestSample.scala#multivaluedQueryParam" scala >}}

If you want to add multiple query parameters at once, there are two suitable methods:

* `queryParamSeq(seq: Expression[Seq[(String, Any)]])`

{{< include-code "HttpRequestSample.scala#queryParamSeq" scala >}}

* `queryParamMap(map: Expression[Map[String, Any]])`

{{< include-code "HttpRequestSample.scala#queryParamMap" scala >}}

{{< alert tip >}}
As all method parameters are `Expression[T]`, i.e. 'key' parameter is an `Expression[String]` and so on, if you have more specific needs you can also provide an arbitrary `Expression[T]`, i.e. a `Session => Validation[T]` function.
This function will be evaluated against the user session every time this one pass through it.
For a deeper look at `Expression` see dedicated section [here]({{< ref "../../session/expression_el" >}}).
{{< /alert >}}

## Headers

HTTP protocol uses headers to exchange information between client and server that is not part of the message (stored in the body of the request, if there is one).

Gatling HTTP allows you to specify any header you want to with the `header(name: String, value: Expression[String])` and `headers(newHeaders: Map[String, String])` methods.

Here are some examples:

{{< include-code "HttpRequestSample.scala#headers" scala >}}

{{< alert tip >}}
Headers keys are defined as constants usable in the scenario, for example: `HttpHeaderNames.ContentType`.
You can find a list of the predefined constants [here](https://github.com/gatling/gatling/blob/master/gatling-http/src/main/scala/io/gatling/http/Headers.scala).
{{< /alert >}}

{{< alert tip >}}
There are two handful methods to help you set the required headers for JSON and XML requests:
{{< /alert >}}

* `http("foo").get("bar").asJson` is equivalent to:

{{< include-code "HttpRequestSample.scala#asJson" scala >}}

* `http("foo").get("bar").asXml` is equivalent to:

{{< include-code "HttpRequestSample.scala#asXml" scala >}}

{{< alert tip >}}
Headers can also be defined on the `HttpProtocol`.
{{< /alert >}}

For a given request, you can also disable common headers that were defined on the `HttpProtocol` with `ignoreProtocolHeaders`:

{{< include-code "HttpRequestSample.scala#ignoreProtocolHeaders" scala >}}

## Request Timeout

The default request timeout is controlled by the ``gatling.http.requestTimeout` configuration parameter.

However, you might want to use `requestTimeout(timeout: FiniteDuration)`
to override the global value for a specific request, typically a long file upload or download.

{{< include-code "HttpRequestSample.scala#requestTimeout" scala >}}

## Signature Calculator

You might want to edit the HTTP requests before they're being sent over the wire, based on other request information: url, headers and/or body.
For example, you might want to generate some [HMAC](http://en.wikipedia.org/wiki/Hash-based_message_authentication_code) header.

This can only happen after Gatling has resolved the request, e.g. computed the body based on a template.

Gatling provides the `SignatureCalculator` API:

```scala
public interface SignatureCalculator {
  void sign(Request request) throws Exception;
}
```

`request` is the mutable object that's been computed so far.
You can typically use its attributes to compute a new header that you will add to the existing headers.

The proper method signature for setting a `SignatureCalculator` is:

```scala
.sign(calculator: Expression[SignatureCalculator])
```

but you can pass a static `SignatureCalculator` instead of an `Expression` and Gatling DSL will automatically lift it for you.

Gatling also provides a built-in for OAuth1:

```
.signWithOAuth1(consumerKey: Expression[String],
               clientSharedSecret: Expression[String],
               token: Expression[String],
               tokenSecret: Expression[String])
```

## Authentication

You can set the authentication methods at request level with these methods:

* `basicAuth(username: Expression[String], password: Expression[String])`
* `digestAuth(username: Expression[String], password: Expression[String])`

{{< include-code "HttpRequestSample.scala#authentication" scala >}}

{{< alert tip >}}
Authentication can also be defined on the `HttpProtocol`.
{{< /alert >}}

## Outgoing Proxy

You can tell Gatling to use a proxy to send the HTTP requests.
You can optionally set a different port for HTTPS and credentials:

{{< include-code "HttpRequestSample.scala#outgoing-proxy" scala >}}

{{< alert tip >}}
Proxy can also be defined on the `HttpProtocol`.
{{< /alert >}}


## Virtual Host

You can tell Gatling to override the default computed virtual host with the method `virtualHost(virtualHost: Expression[String])`:

{{< include-code "HttpRequestSample.scala#virtual-host" scala >}}

{{< alert tip >}}
Virtual Host can also be defined on the `HttpProtocol`.
{{< /alert >}}

## HTTP Checks

You can add checks on a request:

{{< include-code "HttpRequestSample.scala#check" scala >}}

For more information, see the [HTTP Checks reference section]({{< ref "../check" >}}).

For a given request, you can also disable common checks that were defined on the `HttpProtocol` with `ignoreProtocolChecks`:

{{< include-code "HttpRequestSample.scala#ignoreProtocolChecks" scala >}}

## FollowRedirect

For a given request, you can use `disableFollowRedirect`, just like it can be done globally on the `HttpProtocol`:

{{< include-code "HttpRequestSample.scala#disableFollowRedirect" scala >}}

## Url Encoding

Url components are supposed to be [urlencoded](http://www.w3schools.com/tags/ref_urlencode.asp).
Gatling will encode them for you, there might be some corner cases where already encoded components might be encoded twice.

If you know that your urls are already properly encoded, you can disable this feature with `.disableUrlEncoding`.

## Silencing

See [silencing protocol section]({{< ref "../protocol#silencing" >}}) for more details.

You can then make the request *silent*:

{{< include-code "HttpRequestSample.scala#silent" scala >}}

You might also want to do the exact opposite, typically on a given resource while resources have been globally turned silent at protocol level:

{{< include-code "HttpRequestSample.scala#notSilent" scala >}}

## Form Parameters

Requests can have parameters defined in their body.
This is typically used for form submission, where all the values are stored as POST parameters in the body of the request.

To add such parameters to a POST request, you must use the method `formParam(key: Expression[String], value: Expression[Any])` which is actually the same as `queryParam` in **terms of usage** (it has the same signatures).

{{< include-code "HttpRequestSample.scala#formParam" scala >}}

As for `queryParam` you have two methods to add multiple parameters at once:

* `formParamSeq(seq: Expression[Seq[(String, Any)]])`:

{{< include-code "HttpRequestSample.scala#formParamSeq" scala >}}

* `formParamMap(map: Expression[Map[String, Any]])`:

{{< include-code "HttpRequestSample.scala#formParamMap" scala >}}

If you'd like to pass multiple values for your parameter, but all at once, you can use `multivaluedFormParam(key: Expression[String], values: Expression[Seq[Any]])`:

{{< include-code "HttpRequestSample.scala#multivaluedFormParam" scala >}}

The method `formParam` can also take directly an `HttpParam` instance, if you want to build it by hand.

* `form(seq: Expression[Map[String, Any])`:

{{< include-code "HttpRequestSample.scala#form" scala >}}

Typically used after capturing a whole form with a `form` check.

You can override the form field values with the `formParam` and the likes.

{{< alert tip >}}
Gatling will automatically set the `Content-Type` header for you if you didn't specify one.
It will use `application/x-www-form-urlencoded` except if there's also some body parts, in which case it will set `multipart/form-data`.
{{< /alert >}}

.. http-files:

## File Based Request Bodies

Gatling provides various ways of sending files.

When using the bundle distribution, files must be in the `user-files/resources` directory. This location can be overridden, see [configuration`.

When using a build tool such as maven, files must be in `src/main/resources` or `src/test/resources`.

## Multipart Form

This applies only for POST requests. When you find forms asking for text values and a file to upload (usually an email attachment), your browser will send a multipart encoded request.

To define such a request, you have to add the parameters as stated above, and the file to be uploaded at the same time with the following method: `formUpload(name: Expression[String], filePath: Expression[String])`.

The `Content-Type` header will be set to `multipart/form-data` and the file added in addition to the parameters.

One can call `formUpload()` multiple times in order to upload multiple files.

{{< include-code "HttpRequestSample.scala#formUpload" scala >}}

{{< alert tip >}}
Gatling will automatically set the `Content-Type` header to `multipart/form-data` if you didn't specify one.
{{< /alert >}}

{{< alert tip >}}
The MIME Type of the uploaded file defaults to `application/octet-stream` and the character set defaults to the one configured in `gatling.conf` (`UTF-8` by default).
Don't forget to override them when needed.
Then, directly use a body part, e.g. `.bodyPart(RawFileBodyPart("file", data.xls").contentType("application/vnd.ms-excel").fileName("data.xls")).asMultipartForm`.
{{< /alert >}}

{{< alert tip >}}
There is a helpful method to help you deal with multipart form requests: `asMultipartForm`.
It is equivalent to `header(HttpHeaderNames.ContentType, HttpHeaderValues.MultipartFormData)`.
If you use `formUpload` the header is automatically set for you.
{{< /alert >}}

## Request Body

You can add a full body to an HTTP request with the dedicated method `body(body)`, where body can be:

* `RawFileBody(path: Expression[String])` where path is the location of a file that will be uploaded as is

`RawFileBody` lets you pass a raw file that will be sent as is.
Over regular HTTP, Gatling can optimise sending such a body and directly stream from the file to the socket, without copying in memory.
Of course, this optimisation is disabled over HTTPS, as bytes have to be encoded, i.e. loaded in memory.:

{{< include-code "HttpRequestSample.scala#RawFileBody" scala >}}

* `ElFileBody(path: Expression[String])` where path is the location of a file whose content will be parsed and resolved with Gatling EL engine

Here, the file content is parsed and turned into a Gatling EL expression.
Of course, it can't be binary.:

```scala
// myFileBody.json is a file that contains
// { "myContent": "${myDynamicValue}" }
.body(ElFileBody("myFileBody.json")).asJson
```

* `StringBody(string: Expression[String])`

Here, you can pass a raw String, a Gatling EL String, or an Expression function.:

{{< include-code "HttpRequestSample.scala#StringBody" scala >}}

* `ByteArrayBody(bytes: Expression[Array[Byte]])`

Here, you can pass bytes instead of text.

* `InputStreamBody(stream: Expression[InputStream])`

Here, you can pass a Stream.

* `PebbleStringBody(template: String)` and `PebbleFileBody(path: Expression[String])`

Gatling Expression Language is definitively the most optimized templating engine for Gatling, in terms of raw performance. However, it's a bit limited in terms of logic you can implement in there.
If you want loops and conditional blocks, you can use Gatling's [Pebble](https://github.com/PebbleTemplates/pebble) based templating engine.

{{< include-code "HttpRequestSample.scala#PebbleBody" scala >}}

{{< alert tip >}}
Template inheritance is only available when using `PebbleFileBody`.
{{< /alert >}}

{{< alert tip >}}
You can register Pebble `Extensions`s with `registerPebbleExtensions(extensions: Extension*)`. This can only be do once, and must be done prior to loading any Pebble template.
{{< /alert >}}

{{< alert tip >}}
When you pass a path, Gatling searches first for an absolute path on the filesystem, then in the classpath.
{{< /alert >}}

Note that one can take full advantage of Scala 2.10 macros for writing template directly in Scala compiled code instead of relying on a templating engine.
See [Scala 2.10 string interpolation](http://docs.scala-lang.org/overviews/core/string-interpolation.html) and [Fastring](https://github.com/Atry/fastring).

For example:

{{< include-code "HttpRequestSample.scala#templates" scala >}}

{{< alert tip >}}
For simple use cases, prefer EL strings or based files, for more complex ones where programming capability is required, prefer String interpolation or Fastring.
{{< /alert >}}

## Multipart Request

You can add a multipart body to an HTTP request and add parts with the dedicated method `bodyPart(bodyPart)`, where bodyPart can be:

* `RawFileBodyPart(path: Expression[String])`
* `RawFileBodyPart(name: Expression[String], path: Expression[String])`

where path is the location of a file that will be uploaded as is.

Similar to [RawFileBody]({{< ref "#request-body" >}}).

* `ElFileBodyPart(path: Expression[String])`
* `ElFileBodyPart(name: Expression[String], path: Expression[String])`

where path is the location of a file whose content will be parsed and resolved with Gatling EL engine.

Similar to [ElFileBody]({{< ref "#request-body" >}}).

* `StringBodyPart(string: Expression[String])`
* `StringBodyPart(name: Expression[String], string: Expression[String])`

Similar to [StringBody]({{< ref "#request-body" >}}).

* `PebbleStringBodyPart(string: String)`
* `PebbleStringBodyPart(name: Expression[String], string: String)`

Similar to [PebbleStringBody]({{< ref "#request-body" >}}).

* `PebbleFileBodyPart(path: Expression[String])`
* `PebbleFileBodyPart(name: Expression[String], path: Expression[String])`

Similar to [PebbleFileBody]({{< ref "#request-body" >}}).

* `ByteArrayBodyPart(bytes: Expression[Array[Byte])`
* `ByteArrayBodyPart(name: Expression[String], bytes: Expression[Array[Byte])`

Similar to [ByteArrayBody]({{< ref "#request-body" >}}).

Once bootstrapped, BodyPart has the following methods for setting additional optional information:

* `contentType(contentType: String)`
* `charset(charset: String)`, part of of `Content-Type` header. If not set, defaults to the one from `gatling.conf` file.
* `fileName(fileName: Expression[String])`, part of the *Content-Disposition* header.
* `dispositionType(contentId: String)`, part of the `Content-Disposition` header. If not set, defaults to `form-data`.
* `contentId(contentId: Expression[String])`
* `transferEncoding(transferEncoding: String)`
* `header(name: String, value: Expression[String])`, let you define additional part headers

## Request Body Processor

You might want to process the request body before it's being sent to the wire.

`processRequestBody(processor: Body => Body)`: takes a `Body => Body`

Gatling ships two built-ins:

* `gzipBody`: compress the request body with GZIP
* `streamBody`: turn the body into a stream

## Response Transformers

Similarly, one might want to process the response before it's passed to the checks pipeline:

`transformResponse(responseTransformer: (Session => Response) => Validation[Response])`

The example below shows how to decode some Base64 encoded response body:

{{< include-code "HttpRequestSample.scala#resp-processors-imports,response-processors" scala >}}

## Resources

Gatling allow to fetch resources in parallel in order to emulate the behavior of a real web browser.

At the request level you can use the `resources(res: AbstractHttpRequestBuilder[_]*)` method.

For example:

{{< include-code "HttpRequestSample.scala#resources" scala >}}
