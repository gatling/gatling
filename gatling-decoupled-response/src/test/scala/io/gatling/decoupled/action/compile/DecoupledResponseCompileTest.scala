/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.decoupled.action.compile

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.decoupled.Predef._

class DecoupledResponseCompileTest extends Simulation {

  private val httpProtocol = http

  private val sqsProtocol =
    sqs("eu-west-1", "https://sqs.eu-west-1.amazonaws.com/481516234200/queue")
      .awsAccessKeyId("id")
      .awsSecretAccessKey("secret")
      .decoupledResponseTimeoutSeconds(10)
      .processingTimeoutSeconds(5)

  private val decoupledScenario = scenario("Scn")
    .exec(
      decoupledResponse(
        "Test",
        http("Request").get("/")
      ).correlationIdHeaderName("X-CORRELATION-ID")
    )

  setUp(decoupledScenario.inject(atOnceUsers(1))).protocols(httpProtocol, sqsProtocol)

}
