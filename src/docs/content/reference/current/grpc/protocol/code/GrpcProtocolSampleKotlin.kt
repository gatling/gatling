//#imports
import io.gatling.javaapi.grpc.*

import io.gatling.javaapi.grpc.GrpcDsl.*
//#imports

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*

import io.grpc.*

import java.nio.charset.StandardCharsets.UTF_8

class GrpcProtocolSampleKotlin : Simulation() {

  init {
    //#protocol
    val grpcProtocol = grpc
      .forAddress("host", 50051)

    val scn = scenario("Scenario") // etc.

    setUp(
      scn.injectOpen(atOnceUsers(1))
        .protocols(grpcProtocol)
    )
    //#protocol
  }

  init {
    //#forAddress
    grpc.forAddress("host", 50051)
    //#forAddress
    //#forTarget
    grpc.forTarget("dns:///host:50051")
    //#forTarget
    //#asciiHeader
    grpc.asciiHeader("key", "value")
    //#asciiHeader
    //#asciiHeaders
    grpc.asciiHeaders(
      mapOf("key" to "value")
    )
    //#asciiHeaders
    //#binaryHeader
    grpc.binaryHeader("key", "value".toByteArray(UTF_8))
    //#binaryHeader
    //#binaryHeaders
    grpc.binaryHeaders(
      mapOf("key" to "value".toByteArray(UTF_8))
    )
    //#binaryHeaders
    //#header
    grpc.header(
      Metadata.Key.of("key", Metadata.ASCII_STRING_MARSHALLER),
      "value"
    )
    //#header
    //#shareChannel
    grpc.shareChannel()
    //#shareChannel
    //#shareSslContext
    grpc.shareSslContext()
    //#shareSslContext
    //#usePlaintext
    grpc.usePlaintext()
    //#usePlaintext
    //#useInsecureTrustManager
    grpc.useInsecureTrustManager()
    //#useInsecureTrustManager
    //#useStandardTrustManager
    grpc.useStandardTrustManager()
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
    grpc.usePickFirstLoadBalancingPolicy()
    //#usePickFirstLoadBalancingPolicy
    //#usePickRandomLoadBalancingPolicy
    grpc.usePickRandomLoadBalancingPolicy()
    //#usePickRandomLoadBalancingPolicy
    //#useRoundRobinLoadBalancingPolicy
    grpc.useRoundRobinLoadBalancingPolicy()
    //#useRoundRobinLoadBalancingPolicy
    //#useChannelPool
    grpc.useChannelPool(4)
    //#useChannelPool
  }
}
