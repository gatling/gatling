/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
//#imports
import io.gatling.grpc.Predef._
//#imports

import io.grpc._

import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.Executor

class GrpcProtocolSampleScala extends Simulation {

  {
    //#protocol-configuration
    val grpcProtocol = grpc
      .forAddress("host", 50051)

    val scn = scenario("Scenario") // etc.

    setUp(
      scn.inject(atOnceUsers(1))
        .protocols(grpcProtocol)
    )
    //#protocol-configuration
  }

  {
    //#forAddress
    grpc.forAddress("host", 50051)
    //#forAddress
    //#forTarget
    grpc.forTarget("dns:///host:50051")
    //#forTarget
    //#asciiHeader
    grpc
      // with a static header value
      .asciiHeader("key")("value")
      // with a Gatling EL string header value
      .asciiHeader("key")("#{headerValue}")
      // with a function value
      .asciiHeader("key")(session => session("headerValue").as[String])
    //#asciiHeader
    //#asciiHeaders
    grpc.asciiHeaders(
      Map("key" -> "value")
    )
    //#asciiHeaders
    //#binaryHeader
    grpc
      // with a static header value
      .binaryHeader("key")("value".getBytes(UTF_8))
      // with a Gatling EL string header value
      .binaryHeader("key")("#{headerValue}")
      // with a function value
      .binaryHeader("key")(session => session("headerValue").as[Array[Byte]])
    //#binaryHeader
    //#binaryHeaders
    grpc.binaryHeaders(
      Map("key" -> "value".getBytes(UTF_8))
    )
    //#binaryHeaders
    //#header
    val key = Metadata.Key.of("key", Metadata.ASCII_STRING_MARSHALLER)
    grpc
      // with a static header value
      .header[String](key)("value")
      // with a Gatling EL string header value
      .header[String](key)("#{headerValue}")
      // with a function value
      .header(key)(session => session("headerValue").as[String])
    //#header
    //#shareChannel
    grpc.shareChannel
    //#shareChannel
    //#shareSslContext
    grpc.shareSslContext
    //#shareSslContext
    val callCredentials = callCredentialsForUser("")
    //#callCredentials
    grpc
      // with a constant
      .callCredentials(callCredentials)
      // or with an EL string to retrieve CallCredentials already stored in the session
      .callCredentials("#{callCredentials}")
      // or with a function
      .callCredentials { session =>
        val name = session("myUserName").as[String]
        callCredentialsForUser(name)
      }
    //#callCredentials
    val channelCredentials = channelCredentialsForUser("")
    //#channelCredentials
    grpc
      // with a constant
      .channelCredentials(channelCredentials)
      // or with an EL string to retrieve CallCredentials already stored in the session
      .channelCredentials("#{channelCredentials}")
      // or with a function
      .channelCredentials { session =>
        val name = session("myUserName").as[String]
        channelCredentialsForUser(name)
      }
    //#channelCredentials
    //#tlsMutualAuthChannelCredentials
    grpc.channelCredentials(
      TlsChannelCredentials.newBuilder()
        .keyManager(
          ClassLoader.getSystemResourceAsStream("client.crt"),
          ClassLoader.getSystemResourceAsStream("client.key"))
        .trustManager(ClassLoader.getSystemResourceAsStream("ca.crt"))
        .build()
    )
    //#tlsMutualAuthChannelCredentials
    //#insecureTrustManagerChannelCredentials
    TlsChannelCredentials.newBuilder()
      .trustManager(
        io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE.getTrustManagers:_*
      )
    //#insecureTrustManagerChannelCredentials
    //#overrideAuthority
    grpc.overrideAuthority("test.example.com")
    //#overrideAuthority
    //#usePlaintext
    grpc.usePlaintext
    //#usePlaintext
    //#useInsecureTrustManager
    grpc.useInsecureTrustManager
    //#useInsecureTrustManager
    //#useStandardTrustManager
    grpc.useStandardTrustManager
    //#useStandardTrustManager
    //#useCustomCertificateTrustManager
    grpc.useCustomCertificateTrustManager("certificatePath")
    //#useCustomCertificateTrustManager
    //#useCustomLoadBalancingPolicy
    grpc.useCustomLoadBalancingPolicy("pick_first")
    //#useCustomLoadBalancingPolicy
    //#useCustomLoadBalancingPolicy2
    grpc.useCustomLoadBalancingPolicy("pick_first", "{}")
    //#useCustomLoadBalancingPolicy2
    //#usePickFirstLoadBalancingPolicy
    grpc.usePickFirstLoadBalancingPolicy
    //#usePickFirstLoadBalancingPolicy
    //#usePickRandomLoadBalancingPolicy
    grpc.usePickRandomLoadBalancingPolicy
    //#usePickRandomLoadBalancingPolicy
    //#useRoundRobinLoadBalancingPolicy
    grpc.useRoundRobinLoadBalancingPolicy
    //#useRoundRobinLoadBalancingPolicy
    //#useChannelPool
    grpc.useChannelPool(4)
    //#useChannelPool
  }

  private def callCredentialsForUser(name: String): CallCredentials =
    new CallCredentials() {
      override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo, appExecutor: Executor, applier: CallCredentials.MetadataApplier): Unit = {}
    }

  private def channelCredentialsForUser(name: String): ChannelCredentials =
    new ChannelCredentials() {
      override def withoutBearerTokens(): ChannelCredentials = this
    }
}
