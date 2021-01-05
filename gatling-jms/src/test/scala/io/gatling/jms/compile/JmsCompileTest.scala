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

package io.gatling.jms.compile

import javax.jms._

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.jms.Predef._
import io.gatling.jms.protocol.JmsMessageMatcher

object HeaderMatcher extends JmsMessageMatcher {
  override def prepareRequest(msg: Message): Unit = {}
  override def requestMatchId(msg: Message): String = msg.getStringProperty("header")
  override def responseMatchId(msg: Message): String = requestMatchId(msg)
}

class JmsCompileTest extends Simulation {

  // create JmsProtocol from standard ConnectionFactory
  private val jmsProtocolWithNativeConnectionFactory = jms
    .connectionFactory(new org.apache.activemq.ActiveMQConnectionFactory("tcp://localhost:61616"))

  // create JmsProtocol from JNDI based ConnectionFactory
  private val jmsProtocolWithJndiConnectionFactory = jms
    .connectionFactory(
      jmsJndiConnectionFactory
        .connectionFactoryName("ConnectionFactory")
        .url("tcp://localhost:10002")
        .credentials("user", "secret")
        .property("FOO", "BAR")
        .contextFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory")
    )
    .usePersistentDeliveryMode
    .replyTimeout(1000)
    .messageMatcher(HeaderMatcher)
    .matchByCorrelationId
    .matchByMessageId

  private val scn = scenario("scn")
    // requestReply
    // textMessage
    .exec(
      jms("req").requestReply
        .queue("queue")
        .replyQueue("responseQueue")
        .textMessage("hello")
    )
    // textMessage with ElFileBody
    .exec(
      jms("req").requestReply
        .queue("queue")
        .textMessage(ElFileBody("file"))
    )
    // bytesMessage
    .exec(
      jms("req").requestReply
        .queue("queue")
        .bytesMessage(new Array[Byte](1))
    )
    // bytesMessage with RawFileBody
    .exec(
      jms("req").requestReply
        .queue("queue")
        .bytesMessage(RawFileBody("file"))
    )
    // mapMessage
    .exec(
      jms("req").requestReply
        .queue("queue")
        .mapMessage(Map("foo" -> "bar"))
    )
    // objectMessage
    .exec(
      jms("req").requestReply
        .queue("queue")
        .objectMessage("hello")
    )
    // destination
    .exec(
      jms("req").requestReply
        .destination(topic("topic"))
        .noJmsReplyTo
        .replyDestination(queue("queue"))
        .trackerQueue("queue")
        .trackerDestination(topic("topic"))
        .selector("selector")
        .textMessage("hello")
    )
    // check
    .exec(
      jms("req").requestReply
        .queue("queue")
        .textMessage("hello")
        .check(checkBodyTextCorrect)
        .check(xpath("//TEST").saveAs("name"))
        .check(jsonPath("$.foo"))
        .check(jmesPath("[].foo"))
        .check(substring("foo"))
        .check(bodyLength.lte(50))
        .check(bodyBytes.transform(_.length).lte(50))
        .check(bodyString)
        .check(
          bodyString.is("hello"),
          substring("he").count.is(1),
          checkIf(_ => true) {
            jsonPath("$").is("hello")
          }
        )
    )
    // extra
    .exec(
      jms("req").requestReply
        .queue("queue")
        .textMessage("hello")
        .property("header", "value")
        .property("header", "${value}")
        .property("header", _ => "value")
        .jmsType("foo")
    )

    // send
    // textMessage
    .exec(
      jms("req").send
        .queue("queue")
        .textMessage("hello")
    )
    // bytesMessage
    .exec(
      jms("req").send
        .queue("queue")
        .bytesMessage(new Array[Byte](1))
    )
    // objectMessage
    .exec(
      jms("req").send
        .queue("queue")
        .objectMessage("hello")
    )
    // mapMessage
    .exec(
      jms("req").send
        .queue("queue")
        .mapMessage(Map("foo" -> "bar"))
    )
    // destination: topic
    .exec(
      jms("req").send
        .destination(topic("topic"))
        .textMessage("hello")
    )
    // destination: queue
    .exec(
      jms("req").send
        .destination(queue("queue"))
        .textMessage("hello")
    )
    // extra
    .exec(
      jms("req").send
        .queue("queue")
        .textMessage("hello")
        .property("header", "value")
        .property("header", "${value}")
        .property("header", _ => "value")
        .jmsType("foo")
    )

  setUp(scn.inject(rampUsersPerSec(10) to 1000 during (2.minutes)))
    .protocols(jmsProtocolWithNativeConnectionFactory)

  private def checkBodyTextCorrect = simpleCheck {
    case tm: TextMessage => tm.getText == "hello"
    case _               => false
  }
}
