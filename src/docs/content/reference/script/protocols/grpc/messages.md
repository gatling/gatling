---
title: gRPC Messages
seotitle: Gatling gRPC protocol reference - messages
description: Request and response message with Gatling gRPC
lead: Learn how to handle gRPC request and response messages in Gatling simulations
date: 2023-09-07T12:00:00:+0200
lastmod: 2023-09-07T12:00:00:+0200
---

Gatling gRPC requests are constructed based on a `MethodDescriptor` which describes all information required to call the
gRPC method, including the marshalling/unmarshalling of the messages sent to and received from the server.

In the most common use case, gRPC services and messages are described in `.proto` files, messages are serialized using
[Protocol Buffers (protobuf)](https://protobuf.dev/), and the `MethodDescriptor` is available in Java code generated
from the `.proto` file with the Protocol Buffer Compiler (protoc). You can find such examples in
[our samples projects](https://github.com/gatling/gatling-grpc-demo).

However, Gatling gRPC will function with any valid instance of the
[`io.grpc.MethodDescriptor`](https://grpc.github.io/grpc-java/javadoc/io/grpc/MethodDescriptor.html) class. The method
descriptor includes everything needed to call the gRPC method, including the marshallers used to serialize
[request messages](https://grpc.github.io/grpc-java/javadoc/io/grpc/MethodDescriptor.html#getRequestMarshaller()) and
[response messages](https://grpc.github.io/grpc-java/javadoc/io/grpc/MethodDescriptor.html#getResponseMarshaller()).

This allows, for instance:
- Using different code generators (such as [ScalaPB](https://scalapb.github.io/) for Scala code, or
  [Pbandk](https://github.com/streem/pbandk) for a pure Kotlin alternative to the official Kotlin support built into protoc).
- Using other serialization formats than protobuf (e.g. JSON), although other formats are less commonly used with gRPC.
- Manually instantiating a method descriptor in your code, instead of using a description file and a code generator.
- Transforming a method descriptor.

## Using protoc-generated Java {#protoc-java}

You can check out our sample projects for configuration examples
[using Maven](https://github.com/gatling/gatling-grpc-demo/tree/main/java/maven) or
[using Gradle](https://github.com/gatling/gatling-grpc-demo/tree/main/java/gradle).

If we consider the following `.proto` service definition:

```protobuf
syntax = "proto3";

package greeting;

option java_package = "io.gatling.grpc.demo.greeting";
option java_multiple_files = true;

message Greeting {
  string first_name = 1;
  string last_name = 2;
}

message GreetRequest {
  Greeting greeting = 1;
}

message GreetResponse {
  string result = 1;
}

service GreetingService {
  rpc Greet(GreetRequest) returns (GreetResponse) {};
  rpc GreetWithDeadline(GreetRequest) returns (GreetResponse) {};
}
```

Then protoc will generate:

- The Java classes `io.gatling.grpc.demo.greeting.Greeting`, `io.gatling.grpc.demo.greeting.GreetRequest`, and
  `io.gatling.grpc.demo.greeting.GreetResponse`, to represent the messages. They each come with builders, used with the
  static method `newBuilder()`.
- The Java class `io.gatling.grpc.demo.greeting.GreetingServiceGrpc`, to represent the gRPC service. It notably exposes
  static getters for the method descriptors: `getGreetMethod()` and `getGreetWithDeadlineMethod()`.

For example, here is how we can send a `GreetRequest` message to the `Greet` method, and validate the `GreetResponse`
message received in response, using the generated code with the Gatling gRPC DSL:

```java
grpc("John Doe's greet request")
  .unary(GreetingServiceGrpc.getGreetMethod())
  .send(
    GreetRequest.newBuilder()
      .setGreeting(
        Greeting.newBuilder()
          .setFirstName("John")
          .setLastName("Doe")
          .build()
      )
      .build()
  )
  .check(
    response(GreetResponse::getResult).is("Hello John Doe")
  );
```

## Using protoc-generated Java and Kotlin {#protoc-java-kotlin}

The Kotlin support built into protoc relies on the same Java classes used [with pure Java code](
{{< ref "#protoc-java" >}}). However, it adds Kotlin builders for the Java classes, to make them easier to use in your
Kotlin code.

You can check out our sample projects for configuration examples
[using Maven](https://github.com/gatling/gatling-grpc-demo/tree/main/kotlin/maven) or
[using Gradle](https://github.com/gatling/gatling-grpc-demo/tree/main/kotlin/gradle).

If we consider the following `.proto` service definition:

```protobuf
syntax = "proto3";

package greeting;

option java_package = "io.gatling.grpc.demo.greeting";
option java_multiple_files = true;

message Greeting {
  string first_name = 1;
  string last_name = 2;
}

message GreetRequest {
  Greeting greeting = 1;
}

message GreetResponse {
  string result = 1;
}

service GreetingService {
  rpc Greet(GreetRequest) returns (GreetResponse) {};
  rpc GreetWithDeadline(GreetRequest) returns (GreetResponse) {};
}
```

Then protoc will generate:

- The Java classes `io.gatling.grpc.demo.greeting.Greeting`, `io.gatling.grpc.demo.greeting.GreetRequest`, and
  `io.gatling.grpc.demo.greeting.GreetResponse`, to represent the messages. They each come with builders, used with the
  static method `newBuilder()`.
- The Kotlin builder methods `io.gatling.grpc.demo.greeting.greeting()`, `io.gatling.grpc.demo.greeting.greetRequest()`,
  and `io.gatling.grpc.demo.greeting.greetResponse()`.
- The Java class `io.gatling.grpc.demo.greeting.GreetingServiceGrpc`, to represent the gRPC service. It notably exposes
  static getters for the method descriptors: `getGreetMethod()` and `getGreetWithDeadlineMethod()`.

For example, here is how we can send a `GreetRequest` message to the `Greet` method, and validate the `GreetResponse`
message received in response, using the generated code with the Gatling gRPC DSL:

```kotlin
grpc("John Doe's greet request")
  .unary(GreetingServiceGrpc.getGreetMethod())
  .send(
    greetRequest {
      greeting = greeting {
        firstName = "John"
        lastName = "Doe"
      }
    }
  )
  .check(
    response(GreetResponse::getResult).shouldBe("Hello John Doe")
  )
```

## Using scalapb-generated Scala {#protoc-scala}

[Generated Java code]({{< ref "#protoc-java" >}}) can also be used in Gatling Scala simulations. However, it's possible
to generate Scala code instead using [ScalaPB](https://scalapb.github.io/). You can check out our sample project for a
configuration example [using sbt](https://github.com/gatling/gatling-grpc-demo/tree/main/scala/sbt).

If we consider the following `.proto` service definition:

```protobuf
syntax = "proto3";

package greeting;

import "scalapb/scalapb.proto";
option (scalapb.options) = {
  flat_package: true
  package_name: "io.gatling.grpc.demo.greeting"
};

message Greeting {
  string first_name = 1;
  string last_name = 2;
}

message GreetRequest {
  Greeting greeting = 1;
}

message GreetResponse {
  string result = 1;
}

service GreetingService {
  rpc Greet(GreetRequest) returns (GreetResponse) {};
  rpc GreetWithDeadline(GreetRequest) returns (GreetResponse) {};
}
```

Then ScalaPB will generate:

- The Scala case classes `io.gatling.grpc.demo.greeting.Greeting`, `io.gatling.grpc.demo.greeting.GreetRequest`, and
  `io.gatling.grpc.demo.greeting.GreetResponse`, to represent the messages.
- The Scala object `GreetingServiceGrpc`, to represent the gRPC service. It notably exposes fields for the method
  descriptors: `METHOD_GREET` and `METHOD_GREET_WITH_DEADLINE`.

For example, here is how we can send a `GreetRequest` message to the `Greet` method, and validate the `GreetResponse`
message received in response, using the generated code with the Gatling gRPC DSL:

```scala
grpc("John Doe's greet request")
  .unary(GreetingServiceGrpc.METHOD_GREET)
  .send(
    GreetRequest(
      greeting = Some(
        Greeting(
          firstName = "John",
          lastName = "Doe"
        )
      )
    )
  )
  .check(
    response((response: GreetResponse) => response.result)
      .is("Hello John Doe")
  )
```
