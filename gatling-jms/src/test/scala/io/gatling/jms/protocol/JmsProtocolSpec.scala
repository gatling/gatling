/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.jms.protocol

import io.gatling.{ ValidationValues, BaseSpec }
import io.gatling.jms.MockMessage
import io.gatling.jms.Predef._

class JmsProtocolSpec extends BaseSpec with ValidationValues with MockMessage {

  "jms protocol" should "pass defined parameters" in {

    val protocol = jms.connectionFactoryName("cfName").url("url").contextFactory("cf").listenerCount(3).build
    protocol.connectionFactoryName shouldBe "cfName"
    protocol.url shouldBe "url"
    protocol.contextFactory shouldBe "cf"
    protocol.listenerCount shouldBe 3

  }

  it should "pass receiveTimeout" in {
    val protocol =
      jms
        .connectionFactoryName("cfName")
        .url("url")
        .contextFactory("cf")
        .listenerCount(3)
        .receiveTimeout(1500)
        .build
    protocol.receiveTimeout shouldBe Some(1500L)
  }

}
