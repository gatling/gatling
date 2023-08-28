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

//#imports
import io.gatling.javaapi.grpc.*;

import static io.gatling.javaapi.grpc.GrpcDsl.*;
//#imports

import io.gatling.javaapi.core.*;

import io.grpc.*;

import java.util.Collections;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.grpc.GrpcDsl.*;

import static java.nio.charset.StandardCharsets.UTF_8;

class GrpcProtocolSampleJava extends Simulation {

  {
    //#protocol
    GrpcProtocolBuilder grpcProtocol = grpc
      .forAddress("host", 50051);

    ScenarioBuilder scn = scenario("Scenario"); // etc.

    setUp(
      scn.injectOpen(atOnceUsers(1))
        .protocols(grpcProtocol)
    );
    //#protocol
  }

  {
    //#forAddress
    grpc.forAddress("host", 50051);
    //#forAddress
    //#forTarget
    grpc.forTarget("dns:///host:50051");
    //#forTarget
    //#asciiHeader
    grpc.asciiHeader("key", "value");
    //#asciiHeader
    //#asciiHeaders
    grpc.asciiHeaders(
      Collections.singletonMap("key", "value")
    );
    //#asciiHeaders
    //#binaryHeader
    grpc.binaryHeader("key", "value".getBytes(UTF_8));
    //#binaryHeader
    //#binaryHeaders
    grpc.binaryHeaders(
      Collections.singletonMap("key", "value".getBytes(UTF_8))
    );
    //#binaryHeaders
    //#header
    grpc.header(
      Metadata.Key.of("key", Metadata.ASCII_STRING_MARSHALLER),
      "value"
    );
    //#header
    //#shareChannel
    grpc.shareChannel();
    //#shareChannel
    //#shareSslContext
    grpc.shareSslContext();
    //#shareSslContext
    //#usePlaintext
    grpc.usePlaintext();
    //#usePlaintext
    //#useInsecureTrustManager
    grpc.useInsecureTrustManager();
    //#useInsecureTrustManager
    //#useStandardTrustManager
    grpc.useStandardTrustManager();
    //#useStandardTrustManager
    //#useCustomCertificateTrustManager
    grpc.useCustomCertificateTrustManager("certificatePath");
    //#useCustomCertificateTrustManager
    //#useCustomLoadBalancingPolicy
    grpc.useCustomLoadBalancingPolicy("pick_first");
    //#useCustomLoadBalancingPolicy
    //#useCustomLoadBalancingPolicy2
    grpc.useCustomLoadBalancingPolicy("pick_first", "{}");
    //#useCustomLoadBalancingPolicy2
    //#usePickFirstLoadBalancingPolicy
    grpc.usePickFirstLoadBalancingPolicy();
    //#usePickFirstLoadBalancingPolicy
    //#usePickRandomLoadBalancingPolicy
    grpc.usePickRandomLoadBalancingPolicy();
    //#usePickRandomLoadBalancingPolicy
    //#useRoundRobinLoadBalancingPolicy
    grpc.useRoundRobinLoadBalancingPolicy();
    //#useRoundRobinLoadBalancingPolicy
    //#useChannelPool
    grpc.useChannelPool(4);
    //#useChannelPool
  }
}
