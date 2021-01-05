/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.config

import io.gatling.BaseSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.protocol.{ HttpProtocol, HttpProtocolBuilder }

class HttpProtocolBuilderSpec extends BaseSpec {

  private val httpProtocolBuilder = HttpProtocolBuilder(GatlingConfiguration.loadForTest())

  "http protocol configuration builder" should "set a silent URI regex" in {
    val builder = httpProtocolBuilder
      .silentUri(".*")

    val config: HttpProtocol = builder.build

    val actualPattern: String = config.requestPart.silentUri.get.toString
    actualPattern.equals(".*") shouldBe true
  }
}
