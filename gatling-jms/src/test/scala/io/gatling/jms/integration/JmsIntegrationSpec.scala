package io.gatling.jms.integration

import javax.jms.TextMessage

import io.gatling.core.CoreModule
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.test.ActorSupport
import io.gatling.jms.JmsQueue
import io.gatling.jms.Predef._

class JmsIntegrationSpec extends JmsMockingSpec with CoreModule {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "gatling-jms" should "send and receive JMS message" in ActorSupport { implicit testKit =>

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
