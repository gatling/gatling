import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.grpc.*
import io.gatling.javaapi.grpc.GrpcDsl.*

import io.grpc.*

import java.nio.charset.StandardCharsets.UTF_8

class GrpcChecksSampleKotlin : Simulation() {

  private class RequestMessage(val message: String?)
  private class ResponseMessage(val _message: String?) {
    fun getMessage(): String? = _message
  }
  private object ExampleServiceGrpc {
    fun getExampleMethod(): MethodDescriptor<RequestMessage, ResponseMessage> = TODO()
  }

  private val message = RequestMessage("hello")

  // Checks

  init {
    grpc("status checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#statusCode
        statusCode().shouldBe(Status.Code.OK)
        //#statusCode
        ,
        //#statusDescription
        statusDescription().shouldBe("actual status description")
        //#statusDescription
        ,
        //#statusDescriptionIsNull
        statusDescription().isNull()
        //#statusDescriptionIsNull
        ,
        //#statusCause
        statusCause().transform { it.message }.shouldBe("actual cause message")
        //#statusCause
        ,
        //#statusCauseIsNull
        statusCause().isNull()
        //#statusCauseIsNull
      )

    grpc("header checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#header
        header(
          Metadata.Key.of("header", Metadata.ASCII_STRING_MARSHALLER)
        ).shouldBe("value")
        //#header
        ,
        //#headerMultiValued
        header(
          Metadata.Key.of("header", Metadata.ASCII_STRING_MARSHALLER)
        ).findAll().shouldBe(listOf("value one", "value two"))
        //#headerMultiValued
        ,
        //#asciiHeader
        asciiHeader("header").shouldBe("value")
        //#asciiHeader
        ,
        //#binaryHeader
        binaryHeader("header-bin").shouldBe("value".toByteArray(UTF_8))
        //#binaryHeader
      )

    grpc("trailer checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#trailer
        trailer(
          Metadata.Key.of("trailer", Metadata.ASCII_STRING_MARSHALLER)
        ).shouldBe("value")
        //#trailer
        ,
        //#trailerMultiValued
        trailer(
          Metadata.Key.of("trailer", Metadata.ASCII_STRING_MARSHALLER)
        ).findAll().shouldBe(listOf("value one", "value two"))
        //#trailerMultiValued
        ,
        //#asciiTrailer
        asciiTrailer("header").shouldBe("value")
        //#asciiTrailer
        ,
        //#binaryTrailer
        binaryTrailer("trailer-bin").shouldBe("value".toByteArray(UTF_8))
        //#binaryTrailer
      )

    grpc("message checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        //#message
        response(ResponseMessage::getMessage)
          .shouldBe("actual result")
        //#message
      )
  }

  // Priorities and scope

  init {
    //#ordering
    grpc("unary checks")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        response(ResponseMessage::getMessage).shouldBe("message value"),
        asciiTrailer("trailer").shouldBe("trailer value"),
        asciiHeader("header").shouldBe("header value"),
        statusCode().shouldBe(Status.Code.OK)
      )
    //#ordering

    //#unaryChecks
    grpc("unary")
      .unary(ExampleServiceGrpc.getExampleMethod())
      .send(message)
      .check(
        response(ResponseMessage::getMessage).shouldBe("message value")
      )
    //#unaryChecks

    //#serverStreamChecks
    val serverStream =
      grpc("server stream")
        .serverStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          response(ResponseMessage::getMessage).shouldBe("message value")
        )

    exec(
      serverStream.send(message),
      serverStream.awaitStreamEnd()
    )
    //#serverStreamChecks

    //#clientStreamChecks
    val clientStream =
      grpc("client stream")
        .clientStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          response(ResponseMessage::getMessage).shouldBe("message value")
        )

    exec(
      clientStream.start(),
      clientStream.send(message),
      clientStream.halfClose(),
      clientStream.awaitStreamEnd()
    )
    //#clientStreamChecks

    //#bidiStreamChecks
    val bidiStream =
      grpc("bidi stream")
        .bidiStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          response(ResponseMessage::getMessage).shouldBe("message value")
        )

    exec(
      bidiStream.start(),
      bidiStream.send(message),
      bidiStream.halfClose(),
      bidiStream.awaitStreamEnd()
    )
    //#bidiStreamChecks
  }

  init {
    //#reconcile
    val serverStream =
      grpc("server stream")
        .serverStream(ExampleServiceGrpc.getExampleMethod())
        .check(
          // Overwrites the 'result' key for each message received
          response(ResponseMessage::getMessage).saveAs("result")
        )

    exec(
      serverStream.send(message),
      serverStream.awaitStreamEnd { main, forked ->
        // Message checks operate on a forked session, we need
        // to reconcile it with the main session at the end
        main.set("result", forked.getString("result"))
      },
      exec { session ->
        // 'result' contains the last message received
        val result = session.getString("result")
        session
      }
    )
    //#reconcile
  }
}
