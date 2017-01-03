/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.HttpCaches
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.protocol.{ HttpProtocolBuilder, HttpProtocol }
import io.gatling.http.request.ExtraInfo

class HttpProtocolBuilderSpec extends BaseSpec {

  val configuration = GatlingConfiguration.loadForTest()
  val httpCaches = new HttpCaches(configuration)
  val httpEngine = mock[HttpEngine]
  val httpProtocolBuilder = HttpProtocolBuilder(configuration)

  "http protocol configuration builder" should "support an optional extra info extractor" in {

    val expectedExtractor = (extraInfo: ExtraInfo) => Nil

    val builder = httpProtocolBuilder
      .disableWarmUp
      .extraInfoExtractor(expectedExtractor)
    val config: HttpProtocol = builder.build

    config.responsePart.extraInfoExtractor.get shouldBe expectedExtractor
  }

  it should "set a silent URI regex" in {
    val builder = httpProtocolBuilder
      .silentURI(".*")

    val config: HttpProtocol = builder.build

    val actualPattern: String = config.requestPart.silentURI.get.toString
    actualPattern.equals(".*") shouldBe true
  }
}
