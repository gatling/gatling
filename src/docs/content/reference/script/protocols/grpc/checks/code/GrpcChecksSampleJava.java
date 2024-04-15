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

import io.gatling.javaapi.grpc.*;
import io.gatling.javaapi.core.*;

import io.grpc.*;

import java.util.Arrays;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.grpc.GrpcDsl.*;
import static java.nio.charset.StandardCharsets.UTF_8;

class GrpcChecksSampleJava extends Simulation {

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

  private RequestMessage message = new RequestMessage("hello");

  // Checks

  {
    grpc("status checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#statusCode
        statusCode().is(Status.Code.OK)
        //#statusCode
        ,
        //#statusDescription
        statusDescription().is("actual status description")
        //#statusDescription
        ,
        //#statusDescriptionIsNull
        statusDescription().isNull()
        //#statusDescriptionIsNull
        ,
        //#statusCause
        statusCause().transform(Throwable::getMessage).is("actual cause message")
        //#statusCause
        ,
        //#statusCauseIsNull
        statusCause().isNull()
        //#statusCauseIsNull
      );

    grpc("header checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#header
        header(
          Metadata.Key.of("header", Metadata.ASCII_STRING_MARSHALLER)
        ).is("value")
        //#header
        ,
        //#headerMultiValued
        header(
          Metadata.Key.of("header", Metadata.ASCII_STRING_MARSHALLER)
        ).findAll().is(Arrays.asList("value one", "value two"))
        //#headerMultiValued
        ,
        //#asciiHeader
        asciiHeader("header").is("value")
        //#asciiHeader
        ,
        //#binaryHeader
        binaryHeader("header-bin").is("value".getBytes(UTF_8))
        //#binaryHeader
      );

    grpc("trailer checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#trailer
        trailer(
          Metadata.Key.of("trailer", Metadata.ASCII_STRING_MARSHALLER)
        ).is("value")
        //#trailer
        ,
        //#trailerMultiValued
        trailer(
          Metadata.Key.of("trailer", Metadata.ASCII_STRING_MARSHALLER)
        ).findAll().is(Arrays.asList("value one", "value two"))
        //#trailerMultiValued
        ,
        //#asciiTrailer
        asciiTrailer("header").is("value")
        //#asciiTrailer
        ,
        //#binaryTrailer
        binaryTrailer("trailer-bin").is("value".getBytes(UTF_8))
        //#binaryTrailer
      );

    grpc("message checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#message
        response(ResponseMessage::getMessage)
          .is("actual result")
        //#message
      );
  }

  // Priorities and scope

  {
    //#ordering
    grpc("unary checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        response(ResponseMessage::getMessage).is("message value"),
        asciiTrailer("trailer").is("trailer value"),
        asciiHeader("header").is("header value"),
        statusCode().is(Status.Code.OK)
      );
    //#ordering

    //#unaryChecks
    grpc("unary")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        response(ResponseMessage::getMessage).is("message value")
      );
    //#unaryChecks

    //#serverStreamChecks
    GrpcServerStreamingServiceBuilder<RequestMessage, ResponseMessage> serverStream =
      grpc("server stream")
        .serverStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          response(ResponseMessage::getMessage).is("message value")
        );

    exec(
      serverStream.send(message),
      serverStream.awaitStreamEnd()
    );
    //#serverStreamChecks

    //#clientStreamChecks
    GrpcClientStreamingServiceBuilder<RequestMessage, ResponseMessage> clientStream =
      grpc("client stream")
        .clientStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          response(ResponseMessage::getMessage).is("message value")
        );

    exec(
      clientStream.start(),
      clientStream.send(message),
      clientStream.halfClose(),
      clientStream.awaitStreamEnd()
    );
    //#clientStreamChecks

    //#bidiStreamChecks
    GrpcBidirectionalStreamingServiceBuilder<RequestMessage, ResponseMessage> bidiStream =
      grpc("bidi stream")
        .bidiStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          response(ResponseMessage::getMessage).is("message value")
        );

    exec(
      bidiStream.start(),
      bidiStream.send(message),
      bidiStream.halfClose(),
      bidiStream.awaitStreamEnd()
    );
    //#bidiStreamChecks
  }

  {
    //#reconcile
    GrpcServerStreamingServiceBuilder<RequestMessage, ResponseMessage> serverStream =
      grpc("server stream")
        .serverStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          // Overwrites the 'result' key for each message received
          response(ResponseMessage::getMessage).saveAs("result")
        );

    exec(
      serverStream.send(message),
      serverStream.awaitStreamEnd((main, forked) ->
        // Message checks operate on a forked session, we need
        // to reconcile it with the main session at the end
        main.set("result", forked.getString("result"))
      ),
      exec(session -> {
        // 'result' contains the last message received
        var result = session.getString("result");
        return session;
      })
    );
    //#reconcile
  }
}
