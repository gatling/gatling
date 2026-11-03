/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine

import io.gatling.core.actor.ActorSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.Controller
import io.gatling.http.util.SslContextsFactory

class HttpClientFactorySpec extends ActorSpec {
  private val configuration = GatlingConfiguration.loadForTest()

  "HttpClientFactory" should "stop the load generator as crashed on fatal errors instead of exiting the JVM" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val factory =
      new HttpClientFactory(new SslContextsFactory(configuration.ssl, configuration.http.enableHostnameVerification), configuration, controller)
    val error = new OutOfMemoryError("emulated")

    factory.newClientConfig().getFatalErrorHandler.accept(error)

    controller.expectMsgType[Controller.Command.StopLoadGenerator]().reason shouldBe
      Controller.Command.StopLoadGenerator.Reason.Crash.Unexpected(error)
  }
}
