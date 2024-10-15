---
title: Migrating from the community plugin
description: Migration guide from the gatling-grpc community plugin to the official Gatling gRPC support
lead: Learn how to migrate a project using the gatling-grpc community plugin to the official Gatling gRPC support
date: 2024-01-31T13:59:53+0200
lastmod: 2024-01-31T13:59:53+0200
private: true
---

{{< alert info >}}
Migration guide from the gatling-grpc community plugin to the official Gatling gRPC support.
{{< /alert >}}

{{< alert warning >}}
The gRPC protocol is not supported by Gatling JS.
{{< /alert >}}

This page is intended for existing users of the community-maintained
[gRPC plugin for Gatling](https://github.com/phiSgr/gatling-grpc) (created by George Leung),
who want to switch to using the new official gRPC support for Gatling.

We currently support a subset of the original features, do not hesitate to contact us using the Gatling Enterprise
support portal with feature requests for anything we do not cover yet. When do you, please detail your actual use case
so we can find the best way to address it in the Gatling gRPC plugin.

{{< alert warning >}}
You are currently using the community-maintained plugin if your build configuration includes the
[com.github.phisgr:gatling-grpc](https://central.sonatype.com/artifact/com.github.phisgr/gatling-grpc) artifact as a
dependency.
{{< /alert >}}

## License

Make sure you understand [the license and usage limitations]({{< ref "setup/#license" >}}) applicable to the official
gRPC support for Gatling.

## Project setup

Note that it can also be useful to refer to [our demo project]({{< ref "setup#demo-project" >}}) as an example.

### Gatling gRPC dependency

Remove the community plugin dependency:

{{< include-file >}}
1-Gradle: includes/dependencies.gradle.md
2-sbt: includes/dependencies.sbt.md
{{< /include-file >}}

Then add the official Gatling gRPC dependency [as described here]({{< ref "setup#gatling-grpc-dependency" >}}).

### Protobuf code generation

You shouldn't need to change your code generation configuration, but you may want to have a look at
[our documentation]({{< ref "setup#protobuf-codegen" >}}) for reference.

## Imports

Remove imports which start with `com.github.phisgr.gatling`. Use the following imports to access the official gRPC DSL:

{{< include-code "GrpcOfficialSample#imports" java kt scala >}}

## Protocol

One major difference is that, in the community plugin, you need to create your own `ManagedChannelBuilder`:

{{< include-code "GrpcCommunitySample#protocol" java kt scala >}}

In the official gRPC support, the creation of the channel builder is hidden inside the Gatling protocol:

{{< include-code "GrpcOfficialSample#protocol" java kt scala >}}

|                                     | Community Plugin                | Official Plugin                                                                               |
|-------------------------------------|---------------------------------|-----------------------------------------------------------------------------------------------|
| Warm up                             | `disableWarmUp`<br>`warmUpCall` | N/A                                                                                           |
| Eagerly/lazily parse message bodies | `forceParsing`                  | N/A                                                                                           |
| Headers                             | `header`                        | `header`<br>`headers`<br>`asciiHeader`<br>`asciiHeaders`<br>`binaryHeader`<br>`binaryHeaders` |
| Share managed channel between users | `shareChannel`                  | `shareChannel`                                                                                |

## Checks

### Message checks

Replace `extract` and `extractMultiple` with a `response` check:

```scala
.extract(_.message.some)(_.is("actual result"))
```

Becomes:

```scala
.check(
  response((result: ResponseMessage) => result.message).is(3)
)
```

### End checks

Replace `endCheck` with the corresponding `statusCode`, `statusDescription`, `header` and/or `trailer` checks:

```scala
.endCheck(statusCode.is(Status.Code.OK))
```

Becomes

```scala
.check(statusCode.is(Status.Code.OK))
```

{{< alert info >}}
`header` and `trailer` have ASCII and binary shortcuts: {{< ref "checks" >}}
{{< /alert >}}

## Methods

### Instantiate a gRPC request

|                                                 | Community Plugin                                  | Official Plugin                                                                            |
|-------------------------------------------------|---------------------------------------------------|--------------------------------------------------------------------------------------------|
| Unary                                           | `grpc(name).rpc(grpcMethod)`                      | `grpc(name).unary(grpcMethod)`                                                             |
| Server stream                                   | `grpc(name).serverStream(grpcMethod, streamName)` | `grpc(name).serverStream(grpcMethod)`<br>`grpc(name).serverStream(grpcMethod, streamName)` |
| Client stream                                   | `grpc(name).clientStream(grpcMethod, streamName)` | `grpc(name).clientStream(grpcMethod)`<br>`grpc(name).clientStream(grpcMethod, streamName)` |
| Bidirectional stream                            | `grpc(name).bidiStream(grpcMethod, streamName)`   | `grpc(name).bidiStream(grpcMethod)`<br>`grpc(name).bidiStream(grpcMethod, streamName)`     |

For streaming methods, the stream name is optional (but you do need to provide one when the same user keeps more than one stream open in parallel).

### Receiving server-streamed messages

In the community plugin:

- `.reconciliate(waitFor = StreamEnd)` only allows extracting the last message
- you can use `.reconciliate(waitFor = NextMessage)`, but you need to know in advance how many messages to expect
- the session available to `extract` is forked from the main session; a `sessionCombiner` allows you to immediately
  merge data into the main session, each time an `extract` is performed

```scala
val serverStream = grpc("Prime Number Decomposition")
  .serverStream(
    ExampleServiceGrpc.METHOD_EXAMPLE,
    "serverStream"
  )

exec(
  serverStream
    .start(PrimeNumberDecompositionRequest(number = 109987656890L))
    .extract(_.primeFactor.some)(_.saveAs("primeFactor"))
    .sessionCombiner { (main, branch) =>
      val primeFactors = main("primeFactors").as[List[Long]]
      val latestPrimeFactor = branch("primeFactor").as[Long]
      main.set("primeFactors", primeFactors :+ latestPrimeFactor)
        .remove("primeFactor")
    }
)
  // Applies extract + sessionCombiner to the 1st message received:
  .exec(serverStream.reconciliate(waitFor = NextMessage))
  // Applies extract + sessionCombiner to the 2nd message received:
  .exec(serverStream.reconciliate(waitFor = NextMessage))
  // Intermediate messages are ignored.
  // Applies extract + sessionCombiner to the last message received:
  .exec(serverStream.reconciliate(waitFor = StreamEnd))
```

In the official gRPC component:

- `awaitStreamEnd` will apply any `response` checks you have defined to every received message (not just the last one)
- there is currently no `await` method for individual messages
- the session available to the `response` checks is forked from the main session; an optional merge function parameter
  for `awaitStreamEnd` allows you to merge data into the main session once, at the end of the stream

```scala
val serverStream = grpc("Prime Number Decomposition")
  .serverStream(CalculatorServiceGrpc.METHOD_PRIME_NUMBER_DECOMPOSITION)
  .check(
    response((response: PrimeNumberDecompositionResponse) => response.primeFactor)
      .saveAs("primeFactor")
  )

exec(
  serverStream.send(PrimeNumberDecompositionRequest(number = 109987656890L)),
  // Applies the response check to each message received (note: it overwrites the
  // 'primeFactor' variable in the forked session every time, this is currently a
  // limitation).
  // Merges the forked session into the main one only once, at the end.
  serverStream.awaitStreamEnd { (main, forked) =>
    val latestPrimeFactor = forked("primeFactor").as[Long]
    main.set("primeFactor", latestPrimeFactor)
  }
)
```

### Sending client-streamed messages

In the community plugin:

```scala
// client streaming
.exec(clientStream.connect)
  .exec(clientStream.send(ComputeAverageRequest(number = 100)))
  .exec(clientStream.send(ComputeAverageRequest(number = 200)))
  .exec(clientStream.send(ComputeAverageRequest(number = 300)))
  .exec(clientStream.completeAndWait)

// bidirectional streaming
.exec(clientStream.connect)
  .exec(clientStream.send(ComputeAverageRequest(number = 100)))
  .exec(clientStream.send(ComputeAverageRequest(number = 200)))
  .exec(clientStream.send(ComputeAverageRequest(number = 300)))
  // several possibilities when half-closing:
  // - half-close and wait for stream end:
  .exec(clientStream.complete(StreamEnd))
  // - half-close and wait for next message:
  .exec(clientStream.complete(NextMessage)
  // - etc.
```

In the official gRPC component:

```scala
// similar for both client streaming and bidirectional streaming
exec(
  clientStream.start,
  clientStream.send(ComputeAverageRequest(number = 100)),
  clientStream.send(ComputeAverageRequest(number = 200)),
  clientStream.send(ComputeAverageRequest(number = 300)),
  // half-close the stream
  clientStream.halfClose,
  // waiting for stream end is handled separately
  clientStream.awaitStreamEnd
)
```

## Response Time policy

In the community plugin, the response time policy is called a timestamp extractor and must be defined on the `connect`:

```scala
exec(
  bidiStream
    .connect
    .timestampExtractor { (session, message, streamStartTime) =>
      streamStartTime
    }
)
```

In the official gRPC component, the `messageResponseTimePolicy` must be defined before `start`:

```scala
val bidiStream = grpc("Find Maximum")
  .bidiStream(CalculatorServiceGrpc.METHOD_FIND_MAXIMUM)
  .messageResponseTimePolicy(FromStreamStartPolicy)

exec(
  bidirectionalStream.start
)
```

More details on the [available polices in the official documentation]({{< ref "methods#method-response-time" >}}).

## Not supported

- `io.grpc.CallOptions`: we don't intend to support completely arbitrary call options, only a subset:
  - Deadline with `deadlineAfter`
  - Others can be added on customer request
- Dynamic protocol: `target`, `setChannel` and `disposeChannel` do not currently have a replacement
- Dynamic payloads, the following syntax (with `$()`, `:~` and `updateExpr`) is not supported:

```scala
val request: Expression[RequestMessage] =
  RequestMessage.defaultInstance.updateExpr(
    _.message :~ $("message")
  )
```

Note that lenses are a feature of `scalapb`, the following would still work in Scala:

```scala
val request: RequestMessage =
  RequestMessage.defaultInstance.update(
    _.message := "actual message"
  )
```

We recommend using a lambda function instead:

{{< include-code "GrpcOfficialSample#expression" java kt scala >}}

- Silencing a request using `silent` is currently not supported
