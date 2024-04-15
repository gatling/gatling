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

import java.nio.charset.StandardCharsets.UTF_8

class GrpcChecksSampleScala extends Simulation {

  private case class RequestMessage(message: String)

  private case class ResponseMessage(message: String)

  private object ExampleServiceGrpc {
    val METHOD_EXAMPLE: MethodDescriptor[RequestMessage, ResponseMessage] = ???
  }

  private val message = RequestMessage("hello")

  // Checks

  grpc("status checks")
    .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
    .send(message)
    .check(
      //#statusCode
      statusCode.is(Status.Code.OK)
      //#statusCode
      ,
      //#statusDescription
      statusDescription.is("actual status description")
      //#statusDescription
      ,
      //#statusDescriptionIsNull
      statusDescription.isNull
      //#statusDescriptionIsNull
      ,
      //#statusCause
      statusCause.transform(_.getMessage).is("actual cause message")
      //#statusCause
      ,
      //#statusCauseIsNull
      statusCause.isNull
      //#statusCauseIsNull
    )

  grpc("header checks")
    .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
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
      ).findAll.is(List("value one", "value two"))
      //#headerMultiValued
      ,
      //#asciiHeader
      asciiHeader("header").is("value")
      //#asciiHeader
      ,
      //#binaryHeader
      binaryHeader("header-bin").is("value".getBytes(UTF_8))
      //#binaryHeader
    )

  grpc("trailer checks")
    .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
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
      ).findAll.is(List("value one", "value two"))
      //#trailerMultiValued
      ,
      //#asciiTrailer
      asciiTrailer("header").is("value")
      //#asciiTrailer
      ,
      //#binaryTrailer
      binaryTrailer("trailer-bin").is("value".getBytes(UTF_8))
      //#binaryTrailer
    )

  grpc("message checks")
    .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
    .send(message)
    .check(
      //#message
      response((result: ResponseMessage) => result.message)
        .is("actual result")
      //#message
    )

  // Priorities and scope

  {
    //#ordering
    grpc("unary checks")
      .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
      .send(message)
      .check(
        response((result: ResponseMessage) => result.message).is("message value"),
        asciiTrailer("trailer").is("trailer value"),
        asciiHeader("header").is("header value"),
        statusCode.is(Status.Code.OK)
      )
    //#ordering
  }

  {
    //#unaryChecks
    grpc("unary")
      .unary(ExampleServiceGrpc.METHOD_EXAMPLE)
      .send(message)
      .check(
        response((result: ResponseMessage) => result.message).is("message value")
      )
    //#unaryChecks
  }

  {
    //#serverStreamChecks
    val serverStream =
      grpc("server stream")
        .serverStream(ExampleServiceGrpc.METHOD_EXAMPLE)
        .check(
          response((result: ResponseMessage) => result.message).is("message value")
        )

    exec(
      serverStream.send(message),
      serverStream.awaitStreamEnd
    )
    //#serverStreamChecks
  }

  {
    //#clientStreamChecks
    val clientStream =
      grpc("client stream")
        .clientStream(ExampleServiceGrpc.METHOD_EXAMPLE)
        .check(
          response((result: ResponseMessage) => result.message).is("message value")
        )

    exec(
      clientStream.start,
      clientStream.send(message),
      clientStream.halfClose,
      clientStream.awaitStreamEnd
    )
    //#clientStreamChecks
  }

  {
    //#bidiStreamChecks
    val bidiStream =
      grpc("bidi stream")
        .bidiStream(ExampleServiceGrpc.METHOD_EXAMPLE)
        .check(
          response((result: ResponseMessage) => result.message).is("message value")
        )

    exec(
      bidiStream.start,
      bidiStream.send(message),
      bidiStream.halfClose,
      bidiStream.awaitStreamEnd
    )
    //#bidiStreamChecks
  }

  {
    //#reconcile
    val serverStream =
      grpc("server stream")
        .serverStream(ExampleServiceGrpc.METHOD_EXAMPLE)
        .check(
          // Overwrites the 'result' key for each message received
          response((result: ResponseMessage) => result.message).saveAs("result")
        )

    exec(
      serverStream.send(message),
      serverStream.awaitStreamEnd { (main, forked) =>
        // Message checks operate on a forked session, we need
        // to reconcile it with the main session at the end
        main.set("result", forked("result").as[String])
      },
      exec { session =>
        // 'result' contains the last message received
        val result = session("result").as[String]
        session
      }
    )
    //#reconcile
  }
}
