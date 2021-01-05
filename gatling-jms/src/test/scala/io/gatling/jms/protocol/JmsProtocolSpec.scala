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

package io.gatling.jms.protocol

import javax.jms.ConnectionFactory

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.model.Credentials
import io.gatling.core.config.GatlingConfiguration
import io.gatling.jms.MockMessage
import io.gatling.jms.Predef._

class JmsProtocolSpec extends BaseSpec with ValidationValues with MockMessage {

  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private val cf = mock[ConnectionFactory]

  "jms protocol" should "pass defined credentials" in {

    val protocol = jms.connectionFactory(cf).credentials("foo", "bar").build
    protocol.connectionFactory shouldBe cf
    protocol.credentials shouldBe Some(Credentials("foo", "bar"))
  }
}
