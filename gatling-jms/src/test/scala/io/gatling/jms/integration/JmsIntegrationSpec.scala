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

package io.gatling.jms.integration

import java.util.Locale
import javax.jms.TextMessage

import io.gatling.core.CoreDsl
import io.gatling.core.config.GatlingConfiguration
import io.gatling.jms._
import io.gatling.jms.request.JmsQueue

class JmsIntegrationSpec extends JmsSpec with CoreDsl with JmsDsl {

  "gatling-jms" should "send and receive JMS message" in {

    val requestQueue = JmsQueue("request")

    replier(
      requestQueue,
      { case (tm: TextMessage, session) =>
        session.createTextMessage(s"""<response>
                                     |<hello>${tm.getText.toUpperCase(Locale.ROOT)}</hello>
                                     |<property><key>${tm.getStringProperty("key")}</key></property>
                                     |<jmsType>${tm.getJMSType}</jmsType>
                                     |</response>""".stripMargin)
      }
    )

    val session = runScenario(
      scenario("Jms upperCase")
        .exec(_.set("sessionMarker", "test"))
        .exec(
          jms("toUpperCase").requestReply
            .destination(requestQueue)
            .textMessage("hi ${sessionMarker}")
            .property("key", "${sessionMarker} value")
            .jmsType("${sessionMarker} jmsType")
            .check(xpath("/response/hello").find.saveAs("content"))
            .check(xpath("/response/property/key").find.saveAs("propertyValue"))
            .check(xpath("/response/jmsType").find.saveAs("jmsType"))
        )
    )

    session.isFailed shouldBe false
    session("content").as[String] shouldBe "HI TEST"
    session("propertyValue").as[String] shouldBe "test value"
    session("jmsType").as[String] shouldBe "test jmsType"
  }
}
