/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gatling.core.Predef._
import io.gatling.grpc.Predef._

import io.grpc._

import java.nio.charset.StandardCharsets

import scala.concurrent.duration._

class GrpcMethodsSampleScala {

  private case class RequestMessage(message: String)
  private case class ResponseMessage(message: String)
  private object ExampleServiceGrpc {
    val METHOD_EXAMPLE: MethodDescriptor[RequestMessage, ResponseMessage] = ???
  }

  private val grpcProtocol = grpc.forAddress("host", 50051)
  private val message = RequestMessage("hello")
  private val message1 = RequestMessage("hello")
  private val message2 = RequestMessage("hello")

  //#unaryInstantiation
  // with a static value
  grpc("request name").unary(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a Gatling EL string
  grpc("#{requestName}").unary(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a function
  grpc(session => session("requestName").as[String]).unary(ExampleServiceGrpc.METHOD_EXAMPLE)
  //#unaryInstantiation

  //#unaryLifecycle
  val scn = scenario("scenario name").exec(
    // Sends a request and awaits a response, similarly to regular HTTP requests
    grpc("request name")
      .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
      .send(RequestMessage("hello"))
  )
  //#unaryLifecycle

  //#serverStreamInstantiation
  // with a static value
  grpc("request name").serverStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a Gatling EL string
  grpc("#{requestName}").serverStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a function
  grpc(session => session("requestName").as[String]).serverStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  //#serverStreamInstantiation

  {
    //#serverStreamLifecycle
    val stream = grpc("request name").serverStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    val scn = scenario("scenario name").exec(
      stream.send(message),
      stream.awaitStreamEnd
    )
    //#serverStreamLifecycle
  }

  {
    //#serverStreamNames
    val stream1 = grpc("request name")
      // specify streamName initially
      .serverStream(ExampleServiceGrpc.METHOD_EXAMPLE, "first-stream")
    val stream2 = grpc("request name")
      .serverStream(ExampleServiceGrpc.METHOD_EXAMPLE)
      // or use the streamName method
      .streamName("second-stream")

    exec(
      stream1.send(message),
      stream2.send(message)
    )
    // both streams are concurrently open at this point
    //#serverStreamNames
  }

  //#clientStreamInstantiation
  // with a static value
  grpc("request name").clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a Gatling EL string
  grpc("#{requestName}").clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a function
  grpc(session => session("requestName").as[String]).clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  //#clientStreamInstantiation

  {
    //#clientStreamLifecycle
    val stream = grpc("request name").clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    val scn = scenario("scenario name").exec(
        stream.start,
        stream.send(message1),
        stream.send(message2),
        stream.halfClose,
      stream.awaitStreamEnd
    )
    //#clientStreamLifecycle
  }

  {
    //#clientStreamNames
    val stream1 = grpc("request name")
      // specify streamName initially
      .clientStream(ExampleServiceGrpc.METHOD_EXAMPLE, "first-stream")
    val stream2 = grpc("request name")
      .clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)
      // or use the streamName method
      .streamName("second-stream")

    exec(
      stream1.start,
      stream2.start
    )
    // both streams are concurrently open at this point
    //#clientStreamNames
  }

  //#bidiStreamInstantiation
  // with a static value
  grpc("request name").bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a Gatling EL string
  grpc("#{requestName}").bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  // with a function
  grpc(session => session("requestName").as[String]).bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)
  //#bidiStreamInstantiation

  {
    //#bidiStreamLifecycle
    val stream = grpc("request name").bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    val scn = scenario("scenario name").exec(
        stream.start,
        stream.send(message1),
        stream.send(message2),
        stream.halfClose,
      stream.awaitStreamEnd
    )
    //#bidiStreamLifecycle
  }

  {
    //#bidiStreamNames
    val stream1 = grpc("request name")
      // specify streamName initially
      .bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE, "first-stream")
    val stream2 = grpc("request name")
      .bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)
      // or use the streamName method
      .streamName("second-stream")

    exec(
      stream1.start,
      stream2.start
    )
    // both streams are concurrently open at this point
    //#bidiStreamNames
  }

  //#unarySend
  // with a static payload
  grpc("name").unary(ExampleServiceGrpc.METHOD_EXAMPLE)
    .send(RequestMessage("hello"))
  // with a function payload
  grpc("name").unary(ExampleServiceGrpc.METHOD_EXAMPLE)
    .send(session => RequestMessage(session("message").as[String]))
  //#unarySend

  {
    //#clientStreamSend
    val stream = grpc("name").clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    exec(
      stream.send(RequestMessage("first message")),
      stream.send(RequestMessage("second message")),
      stream.send(session => RequestMessage(session("third-message").as[String]))
    )
    //#clientStreamSend
  }

  {
    //#unaryAsciiHeaders
    // Extracting a map of headers allows you to reuse these in several requests
    val sentHeaders = Map(
      "header-1" -> "first value",
      "header-2" -> "second value"
    )

    grpc("name")
      .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
      .send(message)
      // Adds several headers at once
      .asciiHeaders(sentHeaders)
      // Adds another header
      .asciiHeader("header", "value")
    //#unaryAsciiHeaders
  }

  {
    //#unaryBinaryHeaders
    // Extracting a map of headers allows you to reuse these in several requests
    val utf8 = StandardCharsets.UTF_8
    val sentHeaders = Map(
      "header-1" -> "first value".getBytes(utf8),
      "header-2" -> "second value".getBytes(utf8)
    )

    grpc("name")
      .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
      .send(message)
      // Adds several headers at once
      .binaryHeaders(sentHeaders)
      // Adds another header
      .binaryHeader("header", "value".getBytes(utf8))
    //#unaryBinaryHeaders
  }

  //#unaryCustomHeaders
  // Define custom marshallers (implementations not shown here)
  val intToAsciiMarshaller: Metadata.AsciiMarshaller[Int] = ???
  val doubleToBinaryMarshaller: Metadata.BinaryMarshaller[Double] = ???

  grpc("name")
    .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
    .send(message)
    // Add headers one at a time (the type of the value must match the type
    // expected by the Key's serializer, e.g. Int for the first one here)
    .header(Metadata.Key.of("header", intToAsciiMarshaller), 123)
    .header(Metadata.Key.of("header-bin", doubleToBinaryMarshaller), 4.56)
  //#unaryCustomHeaders

  {
    //#clientStreamAsciiHeaders
    val stream = grpc("request name")
      .clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)
      .asciiHeader("header", "value")

    exec(
      stream.start, // Header is sent only once, on stream start
      stream.send(message1),
      stream.send(message2)
    )
    //#clientStreamAsciiHeaders
  }

  //#unaryCallOptions
  grpc("name")
    .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
    .send(message)
    .callOptions(CallOptions.DEFAULT.withDeadlineAfter(500, MILLISECONDS))
  //#unaryCallOptions

  //#unaryChecks
  grpc("name")
    .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
    .send(message)
    .check(
      statusCode.is(Status.Code.OK),
      response((result: ResponseMessage) => result.message).is("hello")
    )
  //#unaryChecks

  //#bidiMessageResponseTimePolicy
  grpc("name").bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)
    // Default: from the start of the entire stream
    .messageResponseTimePolicy(FromStreamStartPolicy)
    // From the time when the last request message was sent
    .messageResponseTimePolicy(FromLastMessageSentPolicy)
    // From the time the previous response message was received
    .messageResponseTimePolicy(FromLastMessageReceivedPolicy)
  //#bidiMessageResponseTimePolicy

  {
    //#clientStreamStart
    val stream = grpc("name").clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    exec(stream.start)
    //#clientStreamStart
  }

  {
    //#clientStreamHalfClose
    val stream = grpc("name").clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    exec(stream.halfClose)
    //#clientStreamHalfClose
  }

  {
    //#bidiStreamWaitEnd
    val stream = grpc("name").bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    exec(stream.awaitStreamEnd)
    //#bidiStreamWaitEnd
  }

  {
    //#bidiStreamCancel
    val stream = grpc("name").bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)

    exec(stream.cancel)
    //#bidiStreamCancel
  }
}
