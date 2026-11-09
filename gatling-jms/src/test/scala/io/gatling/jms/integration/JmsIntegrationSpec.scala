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

package io.gatling.jms.integration

import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._

import io.gatling.commons.stats.KO
import io.gatling.core.CoreDsl
import io.gatling.core.protocol.Protocol
import io.gatling.jms.action.MockStatsEngine
import io.gatling.jms.protocol.{ CorrelationIdMessageMatcher, JmsMessageMatcher }
import io.gatling.jms.request.JmsDestination

import jakarta.jms.{ Message, TextMessage }

class JmsIntegrationSpec extends JmsSpec with CoreDsl {
  "gatling-jms" should "send and receive JMS message" in {
    val requestQueue = JmsDestination.Queue("request")

    replier(
      requestQueue,
      { case (tm: TextMessage, session) =>
        session.createTextMessage(s"""<response>
                                     |<hello>${tm.getText.toUpperCase(Locale.ROOT)}</hello>
                                     |<property><key>${tm.getStringProperty("key")}</key></property>
                                     |<jmsType>${tm.getJMSType}</jmsType>
                                     |<priority>${tm.getJMSPriority}</priority>
                                     |<expiration>${tm.getJMSExpiration}</expiration>
                                     |</response>""".stripMargin)
      }
    )

    val session = runScenario(
      scenario("Jms upperCase")
        .exec(_.set("sessionMarker", "test"))
        .exec(
          jms("toUpperCase").requestReply
            .destination(requestQueue)
            .textMessage("hi #{sessionMarker}")
            .property("key", "#{sessionMarker} value")
            .jmsType("#{sessionMarker} jmsType")
            .priority(7)
            .timeToLive(1.minute)
            .check(xpath("/response/hello").find.saveAs("content"))
            .check(xpath("/response/property/key").find.saveAs("propertyValue"))
            .check(xpath("/response/jmsType").find.saveAs("jmsType"))
            .check(xpath("/response/priority").find.saveAs("priority"))
            .check(xpath("/response/expiration").find.saveAs("expiration"))
        )
    )

    session.isFailed shouldBe false
    session("content").as[String] shouldBe "HI TEST"
    session("propertyValue").as[String] shouldBe "test value"
    session("jmsType").as[String] shouldBe "test jmsType"
    session("priority").as[String] shouldBe "7"
    session("expiration").as[String].toLong should be > 0L
  }

  it should "send the user defined correlation id and match the reply on it" in {
    val requestQueue = JmsDestination.Queue("correlationIdRequest")

    replier(
      requestQueue,
      { case (tm: TextMessage, session) => session.createTextMessage(s"<response><correlationId>${tm.getJMSCorrelationID}</correlationId></response>") }
    )

    val session = runScenario(
      scenario("Jms user correlation id")
        .exec(_.set("id", "my-correlation-id"))
        .exec(
          jms("correlationId").requestReply
            .destination(requestQueue)
            .textMessage("hi")
            .correlationId("#{id}")
            .check(xpath("/response/correlationId").is("my-correlation-id"))
        )
    )

    session.isFailed shouldBe false
  }

  it should "fail a request whose correlation id is already waiting for a reply and keep the in-flight one" in {
    val requestQueue = JmsDestination.Queue("duplicateCorrelationIdRequest")
    val received = new AtomicInteger

    // only reply once both requests have been sent, so they're in flight at the same time
    replier(
      requestQueue,
      { case (_: TextMessage, session) if received.incrementAndGet() == 2 => session.createTextMessage("<response/>") }
    )

    val sessions = runScenarioWithSessions(
      scenario("Jms duplicate correlation id")
        .exec(
          jms("duplicate").requestReply
            .destination(requestQueue)
            .textMessage("#{vu}")
            .correlationId("same-correlation-id")
        ),
      Seq(emptySession.set("vu", 1), emptySession.set("vu", 2)),
      10.seconds,
      Protocol.indexByType(Seq(jmsProtocol))
    )

    sessions.map(session => session("vu").as[Int] -> session.isFailed).toMap shouldBe Map(1 -> false, 2 -> true)
  }

  it should "send the user defined correlation id when matching by message id" in {
    val requestQueue = JmsDestination.Queue("correlationIdByMessageIdRequest")

    replier(
      requestQueue,
      { case (tm: TextMessage, session) => session.createTextMessage(s"<response><correlationId>${tm.getJMSCorrelationID}</correlationId></response>") },
      _.getJMSMessageID
    )

    val session = runScenario(
      scenario("Jms user correlation id by message id")
        .exec(
          jms("correlationIdByMessageId").requestReply
            .destination(requestQueue)
            .textMessage("hi")
            .correlationId("my-correlation-id")
            .check(xpath("/response/correlationId").is("my-correlation-id"))
        ),
      protocols = Protocol.indexByType(Seq(jms.connectionFactory(cf).matchByMessageId))
    )

    session.isFailed shouldBe false
  }

  it should "ignore a reply whose matchId can't be computed and keep receiving the next ones" in {
    val requestQueue = JmsDestination.Queue("throwingResponseMatchIdRequest")
    val responseMatchIdCalls = new AtomicInteger

    replier(
      requestQueue,
      { case (tm: TextMessage, session) => session.createTextMessage(tm.getText) }
    )

    val matcher = new JmsMessageMatcher {
      override def prepareRequest(msg: Message): Unit = CorrelationIdMessageMatcher.prepareRequest(msg)
      override def requestMatchId(msg: Message): String = CorrelationIdMessageMatcher.requestMatchId(msg)
      override def responseMatchId(msg: Message): String = {
        responseMatchIdCalls.incrementAndGet()
        msg match {
          case tm: TextMessage if tm.getText == "boom" => throw new IllegalStateException("boom")
          case _                                       => msg.getJMSCorrelationID
        }
      }
    }

    val session = runScenario(
      scenario("Jms throwing responseMatchId")
        .exec(_.set("failedAfterBoom", false))
        .exec(jms("boom").requestReply.destination(requestQueue).textMessage("boom"))
        .exec(session => session.set("failedAfterBoom", session.isFailed).markAsSucceeded)
        .exec(jms("ok").requestReply.destination(requestQueue).textMessage("ok")),
      protocols = Protocol.indexByType(Seq(jms.connectionFactory(cf).messageMatcher(matcher).replyTimeout(3.seconds)))
    )

    session("failedAfterBoom").as[Boolean] shouldBe true
    session.isFailed shouldBe false
    // a listener exception would make the provider redeliver the reply
    responseMatchIdCalls.get shouldBe 2
  }

  it should "fail with the error when the matchId of a sent request can't be computed" in {
    val requestQueue = JmsDestination.Queue("throwingRequestMatchIdRequest")
    val requestMatchIdCalls = new AtomicInteger

    val matcher = new JmsMessageMatcher {
      override def prepareRequest(msg: Message): Unit = {}
      // null before sending, so it's called again once sent
      override def requestMatchId(msg: Message): String =
        if (requestMatchIdCalls.incrementAndGet() == 1) null else throw new IllegalStateException("boom")
      override def responseMatchId(msg: Message): String = msg.getJMSCorrelationID
    }

    val statsEngine = new MockStatsEngine

    val session = runScenario(
      scenario("Jms throwing requestMatchId")
        .exec(jms("throwingRequestMatchId").requestReply.destination(requestQueue).textMessage("hi")),
      protocols = Protocol.indexByType(Seq(jms.connectionFactory(cf).messageMatcher(matcher))),
      statsEngine = statsEngine
    )

    session.isFailed shouldBe true
    statsEngine.messages should matchPattern {
      case List(MockStatsEngine.Message.Response(_, _, "throwingRequestMatchId", _, _, KO, None, Some(message))) if message.contains("boom") =>
    }
  }
}
