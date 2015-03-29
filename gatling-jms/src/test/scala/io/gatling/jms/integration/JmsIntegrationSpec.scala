package io.gatling.jms.integration

import javax.jms.TextMessage

import io.gatling.core.CoreModule
import io.gatling.core.config.GatlingConfiguration
import io.gatling.jms._

class JmsIntegrationSpec extends JmsMockingSpec with CoreModule with JmsModule {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "gatling-jms" should "send and receive JMS message" in {

    val requestQueue = JmsQueue("request")

    jmsMock(requestQueue, {
      case tm: TextMessage => tm.getText.toUpperCase
    })

    val session = runScenario(
      scenario("Jms upperCase")
        .exec(
          jms("toUpperCase")
            .reqreply
            .destination(requestQueue)
            .textMessage("<hello>hi</hello>")
            .check(xpath("/HELLO").find.saveAs("content"))))

    session.isFailed shouldBe false
    session("content").as[String] shouldBe "HI"
  }
}
