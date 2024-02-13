---
title: gRPC Checks
seotitle: Gatling gRPC protocol reference - checks
description: Checks specific to the Gatling gRPC protocol
lead: Checks specific to the Gatling gRPC protocol
date: 2023-08-24T11:27:53:+0200
lastmod: 2024-01-17T10:54:53:+0200
---

## Checks

The following components of a gRPC request can be checked, each having their own built-in:

- Status code and description
- Header(s)
- Trailer(s)
- The returning response(s) or message(s)

### Status

A gRPC status is composed of two parts:

- The status code, defined by the enumeration `io.grpc.Status.Code`
- A description {{< badge info optional />}}

#### `statusCode`

Targets the status code itself:

{{< include-code "statusCode" java kt scala >}}

{{< alert tip >}}
Status is the only check that has defaults in the gRPC protocol.

If you don't define an explicit status code check on gRPC requests or gRPC protocol, Gatling will perform an implicit
check that will verify that the response status code is `Status.Code.OK`.
{{< /alert >}}

#### `statusDescription`

Targets the description part of a status:

{{< include-code "statusDescription" java kt scala >}}

The description being optional, its absence can be tested using `isNull`:

{{< include-code "statusDescriptionIsNull" java kt scala >}}

### Headers

With [gRPC Java](https://github.com/grpc/grpc-java),
headers are defined as [metadata](https://grpc.io/docs/guides/metadata/), i.e., a key-value pair.

#### `header`

To check a header value, it is required to use a `Metadata.Key` as defined in the `io.grpc` package. Here using the
the header name and the default ASCII marshaller:

{{< include-code "header" java kt scala >}}

A header can be multivalued and checked against a collection:

{{< include-code "headerMultiValued" java kt scala >}}

Shortcuts exists to help the usage of ASCII/binary headers that uses the default marshallers.

#### `asciiHeader`

Shortcut for a header with the default ASCII marshaller, i.e. `io.grpc.Metadata#ASCII_STRING_MARSHALLER`:

{{< include-code "asciiHeader" java kt scala >}}

#### `binaryHeader`

And here with the default binary marshaller, i.e. `io.grpc.Metadata#BINARY_BYTE_MARSHALLER`:

{{< include-code "binaryHeader" java kt scala >}}

{{< alert tip >}}
gRPC requires binary header keys to end with the suffix `-bin`.
{{< /alert >}}

### Trailers

#### `trailer`

To check a trailer value, it is required to use a `Metadata.Key` as defined in the `io.grpc` package. Here using the
trailer name and the default ASCII marshaller:

{{< include-code "trailer" java kt scala >}}

A trailer can be multivalued and checked against a collection:

{{< include-code "trailerMultiValued" java kt scala >}}

Shortcuts exists to help the usage of ASCII/binary trailers that uses the default marshallers.

#### `asciiTrailer`

Shortcut for a trailer with the default ASCII marshaller, i.e. `io.grpc.Metadata#ASCII_STRING_MARSHALLER`:

{{< include-code "asciiTrailer" java kt scala >}}

#### `binaryTrailer`

And here with the default binary marshaller:

{{< include-code "binaryTrailer" java kt scala >}}

{{< alert tip >}}
gRPC requires binary trailer keys to end with the suffix `-bin`.
{{< /alert >}}

### Messages

#### `response`

Targets the message part. 

{{< include-code "message" java kt scala >}}

Note that the lambda's parameter type cannot be inferred by the compiler and must be specified explicitly.

## Priorities

Checks are performed in the following order independently of the order in which they are defined:

- Status
- Headers
- Trailers
- Response (Message) 

In the following example, even though the status check is defined last, it will be performed first:

{{< include-code "ordering" java kt scala >}} 

If you don't define a status check yourself, the [default status code check]({{< ref "#statuscode" >}})
will be applied first.

## Scope by gRPC method kind

### Unary

For unary calls, checks are defined after the `send` method:

{{< include-code "unaryChecks" java kt scala >}}

### Streams

For all stream types, checks are defined at the same time as the stream: before the stream is started and/or before
message(s) are sent.

Status, headers and trailers checks are applied only once per stream. Message checks are applied every time a message is
received.

With a server stream:

{{< include-code "serverStreamChecks" java kt scala >}}

A client stream:

{{< include-code "clientStreamChecks" java kt scala >}}

And a bidi stream:

{{< include-code "bidiStreamChecks" java kt scala >}}

## Limitations to the gRPC Checks API

It is not currently possible to apply different checks to specific incoming messages in the same stream. Be wary that
`saveAs` will overwrite previously saved values:

{{< include-code "reconcile" java kt scala >}}
