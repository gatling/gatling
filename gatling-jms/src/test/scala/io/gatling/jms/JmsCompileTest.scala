/*
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
package io.gatling.jms

import javax.jms._

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.jms.Predef._
import io.gatling.jms.protocol.JmsMessageMatcher

object IdentificationMatcher extends JmsMessageMatcher {
  override def prepareRequest(msg: Message): Unit = {}
  override def responseMatchId(msg: Message): String = requestMatchId(msg)
  override def requestMatchId(msg: Message): String = msg.getStringProperty("identification")
}

class JmsCompileTest extends Simulation {

  // create JmsProtocol from standard ConnectionFactory
  val jmsProtocolWithNativeConnectionFactory = jms
    .connectionFactory(new org.apache.activemq.ActiveMQConnectionFactory("tcp://localhost:61616"))

  // create JmsProtocol from JNDI based ConnectionFactory
  val jmsProtocolWithJndiConnectionFactory = jms
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
    .messageMatcher(IdentificationMatcher)

  val scn = scenario("JMS DSL test")
    .repeat(1) {
      exec(jms("req reply testing").requestReply
        .queue("jmstestq")
        // -- four message types are supported; only StreamMessage is not currently supported
        .textMessage("hello from gatling jms dsl")
        .property("test_header", "test_value")
        .check(checkBodyTextCorrect))
        .exec(jms("req reply testing").requestReply
          .queue("jmstestq")
          .bytesMessage(new Array[Byte](1))
          .property("test_header", "test_value")
          .check(checkBodyTextCorrect))
        .exec(jms("req reply testing").requestReply
          .queue("jmstestq")
          .mapMessage(Map("foo" -> "bar"))
          .property("test_header", "test_value")
          .check(checkBodyTextCorrect))
        .exec(jms("req reply testing").requestReply
          .queue("jmstestq")
          .objectMessage("hello!")
          .property("test_header", "test_value")
          .check(checkBodyTextCorrect))
        .exec(jms("req reply - custom").requestReply
          .queue("requestQueue")
          .replyQueue("responseQueue")
          .textMessage("hello from gatling jms dsl")
          .property("identification", "${ID}")
          .check(checkBodyTextCorrect))
    }

  val scnExtra = scenario("JMS DSL using destinations").repeat(1) {
    exec(jms("req reply testing").requestReply
      .destination(topic("jmstesttopic"))
      .textMessage("hello from gatling jms dsl")
      .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").requestReply
        .destination(queue("jmstestq"))
        .replyDestination(queue("jmstestq"))
        .textMessage("hello from gatling jms dsl")
        .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").requestReply
        .destination(topic("requestTopic"))
        .replyDestination(topic("replyTopic")).selector("env='myenv'")
        .textMessage("hello from gatling jms dsl")
        .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").requestReply
        .destination(topic("requestTopic"))
        .replyDestination(topic("replyTopic")).selector("env='myenv'")
        .textMessage("<test>name</test>")
        .check(xpath("//TEST").saveAs("name")))
  }

  val scnSend = scenario("JMS DSL test").repeat(1) {
    exec(jms("req reply testing").send
      .queue("jmstestq")
      // -- four message types are supported; only StreamMessage is not currently supported
      .textMessage("hello from gatling jms dsl")
      .property("test_header", "test_value"))
      .exec(jms("req reply testing").send
        .queue("jmstestq")
        .bytesMessage(new Array[Byte](1))
        .property("test_header", "test_value"))
      .exec(jms("req reply testing").send
        .queue("jmstestq")
        .objectMessage("hello!")
        .property("test_header", "test_value"))
      .exec(jms("req reply - custom").send
        .queue("requestQueue")
        .textMessage("hello from gatling jms dsl")
        .property("identification", "${ID}"))
  }

  val scnSendExtra = scenario("JMS DSL using destinations").repeat(1) {
    exec(jms("req reply testing").send
      .destination(topic("jmstesttopic"))
      .textMessage("hello from gatling jms dsl"))
      .exec(jms("req reply testing").send
        .destination(queue("jmstestq"))
        .textMessage("hello from gatling jms dsl"))
      .exec(jms("req reply testing").send
        .destination(topic("requestTopic"))
        .textMessage("hello from gatling jms dsl"))
      .exec(jms("req reply testing").send
        .destination(topic("requestTopic"))
        .textMessage("<test>name</test>"))
  }

  setUp(scn.inject(rampUsersPerSec(10) to 1000 during (2 minutes)))
    .protocols(jmsProtocolWithNativeConnectionFactory)

  def checkBodyTextCorrect = simpleCheck {
    case tm: TextMessage => tm.getText == "HELLO FROM GATLING JMS DSL"
    case _               => false
  }
}
