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

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.grpc.*;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.grpc.GrpcBidirectionalStreamingServiceBuilder;
import io.gatling.javaapi.grpc.GrpcClientStreamingServiceBuilder;
import io.gatling.javaapi.grpc.GrpcProtocolBuilder;
import io.gatling.javaapi.grpc.GrpcServerStreamingServiceBuilder;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.grpc.GrpcDsl.*;

class GrpcMethodsSampleJava {

  private static class RequestMessage {
    public RequestMessage(String message) {
      this.message = message;
    }
    private String message;
    public String getMessage() {
      return message;
    }
  }
  private static class ResponseMessage {
    public ResponseMessage(String message) {
      this.message = message;
    }
    private String message;
    public String getMessage() {
      return message;
    }
  }
  private static class ExampleServiceGrpc {
    public static MethodDescriptor<RequestMessage, ResponseMessage> getExampleMethod() {
      return null;
    }
  }

  private GrpcProtocolBuilder grpcProtocol = grpc.forAddress("host", 50051);
  private RequestMessage message = new RequestMessage("hello");
  private RequestMessage message1 = new RequestMessage("hello");
  private RequestMessage message2 = new RequestMessage("hello");

  {
    //#unaryInstantiation
    // with a static value
    grpc("request name").unary(ExampleServiceGrpc.getExampleMethod());
    // with a Gatling EL string
    grpc("#{requestName}").unary(ExampleServiceGrpc.getExampleMethod());
    // with a function
    grpc(session -> session.getString("requestName")).unary(ExampleServiceGrpc.getExampleMethod());
    //#unaryInstantiation

    //#unaryLifecycle
    ScenarioBuilder scn = scenario("scenario name").exec(
      // Sends a request and awaits a response, similarly to regular HTTP requests
      grpc("request name")
        .unary(ExampleServiceGrpc.getExampleMethod())
        .send(new RequestMessage("hello"))
    );
    //#unaryLifecycle
  }

  private class ServerStreamInstantiation {{
    //#serverStreamInstantiation
    // with a static value
    grpc("request name").serverStream(ExampleServiceGrpc.getExampleMethod());
    // with a Gatling EL string
    grpc("#{requestName}").serverStream(ExampleServiceGrpc.getExampleMethod());
    // with a function
    grpc(session -> session.getString("requestName")).serverStream(ExampleServiceGrpc.getExampleMethod());
    //#serverStreamInstantiation

    //#serverStreamLifecycle
    GrpcServerStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("request name").serverStream(ExampleServiceGrpc.getExampleMethod());

    ScenarioBuilder scn = scenario("scenario name").exec(
      stream.send(message),
      stream.awaitStreamEnd()
    );
    //#serverStreamLifecycle

    //#serverStreamNames
    GrpcServerStreamingServiceBuilder<RequestMessage, ResponseMessage> stream1 =
      // specify streamName initially
      grpc("request name").serverStream(ExampleServiceGrpc.getExampleMethod(), "first-stream");
    GrpcServerStreamingServiceBuilder<RequestMessage, ResponseMessage> stream2 =
      grpc("request name")
        .serverStream(ExampleServiceGrpc.getExampleMethod())
        // or use the streamName method
        .streamName("second-stream");

    exec(
      stream1.send(message),
      stream2.send(message)
    );
    // both streams are concurrently open at this point
    //#serverStreamNames
  }}


  private class ClientStreamInstantiation {{
    //#clientStreamInstantiation
    // with a static value
    grpc("request name").clientStream(ExampleServiceGrpc.getExampleMethod());
    // with a Gatling EL string
    grpc("#{requestName}").clientStream(ExampleServiceGrpc.getExampleMethod());
    // with a function
    grpc(session -> session.getString("requestName")).clientStream(ExampleServiceGrpc.getExampleMethod());
    //#clientStreamInstantiation

    //#clientStreamLifecycle
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("request name").clientStream(ExampleServiceGrpc.getExampleMethod());

    ScenarioBuilder scn = scenario("scenario name").exec(
      stream.start(),
      stream.send(message1),
      stream.send(message2),
      stream.halfClose(),
      stream.awaitStreamEnd()
    );
    //#clientStreamLifecycle

    //#clientStreamNames
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> stream1 =
      // specify streamName initially
      grpc("request name").clientStream(ExampleServiceGrpc.getExampleMethod(), "first-stream");
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> stream2 =
      grpc("request name")
        .clientStream(ExampleServiceGrpc.getExampleMethod())
        // or use the streamName method
        .streamName("second-stream");

    exec(
      stream1.start(),
      stream2.start()
    );
    // both streams are concurrently open at this point
    //#clientStreamNames
  }}

  private class BidiStreamInstantiation {{
    //#bidiStreamInstantiation
    // with a static value
    grpc("request name").bidiStream(ExampleServiceGrpc.getExampleMethod());
    // with a Gatling EL string
    grpc("#{requestName}").bidiStream(ExampleServiceGrpc.getExampleMethod());
    // with a function
    grpc(session -> session.getString("requestName")).bidiStream(ExampleServiceGrpc.getExampleMethod());
    //#bidiStreamInstantiation

    //#bidiStreamLifecycle
    GrpcBidirectionalStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("request name").bidiStream(ExampleServiceGrpc.getExampleMethod());

    ScenarioBuilder scn = scenario("scenario name").exec(
      stream.start(),
      stream.send(message1),
      stream.send(message2),
      stream.halfClose(),
      stream.awaitStreamEnd()
    );
    //#bidiStreamLifecycle

    //#bidiStreamNames
    GrpcBidirectionalStreamingServiceBuilder<RequestMessage, ResponseMessage> stream1 =
      // specify streamName initially
      grpc("request name").bidiStream(ExampleServiceGrpc.getExampleMethod(), "first-stream");
    GrpcBidirectionalStreamingServiceBuilder<RequestMessage, ResponseMessage> stream2 =
      grpc("request name")
        .bidiStream(ExampleServiceGrpc.getExampleMethod())
        // or use the streamName method
        .streamName("second-stream");

    exec(
      stream1.start(),
      stream2.start()
    );
    // both streams are concurrently open at this point
    //#bidiStreamNames
  }}

  {
    //#unarySend
    // with a static payload
    grpc("name").unary(ExampleServiceGrpc.getExampleMethod())
      .send(new RequestMessage("hello"));
    // with a function payload
    grpc("name").unary(ExampleServiceGrpc.getExampleMethod())
      .send(session -> new RequestMessage(session.getString("message")));
    //#unarySend
  }

  private class ClientStreamSend {{
    //#clientStreamSend
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("name").clientStream(ExampleServiceGrpc.getExampleMethod());

    exec(
      stream.send(new RequestMessage("first message")),
      stream.send(new RequestMessage("second message")),
      stream.send(session -> new RequestMessage(session.getString("third-message")))
    );
    //#clientStreamSend
  }}

  private class UnaryAsciiHeaders {{
    //#unaryAsciiHeaders
    // Extracting a map of headers allows you to reuse these in several requests
    Map<String, String> sentHeaders = new HashMap<>();
    sentHeaders.put("header-1", "first value");
    sentHeaders.put("header-2", "second value");

    grpc("name")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      // Adds several headers at once
      .asciiHeaders(sentHeaders)
      // Adds another header
      .asciiHeader("header", "value");
    //#unaryAsciiHeaders
  }}

  private class UnaryBinaryHeaders {{
    //#unaryBinaryHeaders
    // Extracting a map of headers allows you to reuse these in several requests
    Charset utf8 = StandardCharsets.UTF_8;
    Map<String, byte[]> sentHeaders = new HashMap<>();
    sentHeaders.put("header-1-bin", "first value".getBytes(utf8));
    sentHeaders.put("header-2-bin", "second value".getBytes(utf8));

    grpc("name")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      // Adds several headers at once
      .binaryHeaders(sentHeaders)
      // Adds another header
      .binaryHeader("header-bin", "value".getBytes(utf8));
    //#unaryBinaryHeaders
  }}

  private class UnaryCustomHeaders {{
    //#unaryCustomHeaders
    // Define custom marshallers (implementations not shown here)
    Metadata.AsciiMarshaller<Integer> intToAsciiMarshaller = null;
    Metadata.BinaryMarshaller<Double> doubleToBinaryMarshaller = null;

    grpc("name")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      // Add headers one at a time (the type of the value must match the type
      // expected by the Key's serializer, e.g. Integer for the first one here)
      .header(Metadata.Key.of("header", intToAsciiMarshaller), 123)
      .header(Metadata.Key.of("header-bin", doubleToBinaryMarshaller), 4.56);
    //#unaryCustomHeaders
  }}

  private class ClientStreamAsciiHeaders {{
    //#clientStreamAsciiHeaders
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("name")
        .clientStream(ExampleServiceGrpc.getExampleMethod())
        .asciiHeader("header", "value");

    exec(
      stream.start(), // Header is sent only once, on stream start
      stream.send(message1),
      stream.send(message2)
    );
    //#clientStreamAsciiHeaders
  }}

  private CallCredentials callCredentialsForUser(String name) {
    return new CallCredentials() {
      @Override public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {}
    };
  }
  {
    var callCredentials = callCredentialsForUser("");
    //#unaryCallCredentials
    grpc("name")
        .unary(ExampleServiceGrpc.getExampleMethod())
        .send(message)
        // with a constant
        .callCredentials(callCredentials)
        // or with an EL string to retrieve CallCredentials already stored in the session
        .callCredentials("#{callCredentials}")
        // or with a function
        .callCredentials(session -> {
          var name = session.getString("myUserName");
          return callCredentialsForUser(name);
        });
    //#unaryCallCredentials
  }

  {
    //#deadline
    grpc("name")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      // with a number of seconds
      .deadlineAfter(10)
      // or with a java.time.Duration
      .deadlineAfter(Duration.ofSeconds(10));
    //#deadline
  }

  {
    //#unaryChecks
    grpc("name")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        statusCode().is(Status.Code.OK),
        response(ResponseMessage::getMessage).is("hello")
      );
    //#unaryChecks

    //#bidiMessageResponseTimePolicy
    grpc("name").bidiStream(ExampleServiceGrpc.getExampleMethod())
      // Default: from the start of the entire stream
      .messageResponseTimePolicy(MessageResponseTimePolicy.FromStreamStart)
      // From the time when the last request message was sent
      .messageResponseTimePolicy(MessageResponseTimePolicy.FromLastMessageSent)
      // From the time the previous response message was received
      .messageResponseTimePolicy(MessageResponseTimePolicy.FromLastMessageReceived);
    //#bidiMessageResponseTimePolicy
  }

  private class ClientStreamStart {{
    //#clientStreamStart
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("name").clientStream(ExampleServiceGrpc.getExampleMethod());

    exec(stream.start());
    //#clientStreamStart
  }}

  private class ClientStreamHalfClose {{
    //#clientStreamHalfClose
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("name").clientStream(ExampleServiceGrpc.getExampleMethod());

    exec(stream.halfClose());
    //#clientStreamHalfClose
  }}

  private class BidiStreamWaitEnd {{
    //#bidiStreamWaitEnd
    GrpcBidirectionalStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("name").bidiStream(ExampleServiceGrpc.getExampleMethod());

    exec(stream.awaitStreamEnd());
    //#bidiStreamWaitEnd
  }}

  private class BidiStreamCancel {{
    //#bidiStreamCancel
    GrpcBidirectionalStreamingServiceBuilder<RequestMessage, ResponseMessage> stream =
      grpc("name").bidiStream(ExampleServiceGrpc.getExampleMethod());

    exec(stream.cancel());
    //#bidiStreamCancel
  }}
}
