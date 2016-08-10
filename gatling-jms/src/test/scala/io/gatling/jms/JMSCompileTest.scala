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
package io.gatling.jms

import scala.concurrent.duration.DurationInt

import io.gatling.core.Predef._
import io.gatling.jms.Predef._
import javax.jms._

import io.gatling.jms.protocol.JmsMessageMatcher

object IdentificationMatcher extends JmsMessageMatcher {
  override def prepareRequest(msg: Message): Unit = {}
  override def responseMatchId(msg: Message): String = requestMatchId(msg)
  override def requestMatchId(msg: Message): String = msg.getStringProperty("identification")
}

class JMSCompileTest extends Simulation {

  val jmsConfig = jms
    .connectionFactoryName("FFMQConstants.JNDI_CONNECTION_FACTORY_NAME")
    .url("tcp://localhost:10002")
    .credentials("user", "secret")
    .contextFactory("FFMQConstants.JNDI_CONTEXT_FACTORY")
    .listenerCount(1)
    .usePersistentDeliveryMode
    .receiveTimeout(1000)
    .messageMatcher(IdentificationMatcher)

  val scn = scenario("JMS DSL test").repeat(1) {
    exec(jms("req reply testing").reqreply
      .queue("jmstestq")
      // -- four message types are supported; only StreamMessage is not currently supported
      .textMessage("hello from gatling jms dsl")
      .property("test_header", "test_value")
      .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").reqreply
        .queue("jmstestq")
        .bytesMessage(new Array[Byte](1))
        .property("test_header", "test_value")
        .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").reqreply
        .queue("jmstestq")
        .mapMessage(Map("foo" -> "bar"))
        .property("test_header", "test_value")
        .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").reqreply
        .queue("jmstestq")
        .objectMessage("hello!")
        .property("test_header", "test_value")
        .check(checkBodyTextCorrect))
      .exec(jms("req reply - custom").reqreply
        .queue("requestQueue")
        .replyQueue("responseQueue")
        .textMessage("hello from gatling jms dsl")
        .property("identification", "${ID}")
        .check(checkBodyTextCorrect))
  }

  val scnExtra = scenario("JMS DSL using destinations").repeat(1) {
    exec(jms("req reply testing").reqreply
      .destination(topic("jmstesttopic"))
      .textMessage("hello from gatling jms dsl")
      .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").reqreply
        .destination(queue("jmstestq"))
        .replyDestination(queue("jmstestq"))
        .textMessage("hello from gatling jms dsl")
        .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").reqreply
        .destination(topic("requestTopic"))
        .replyDestination(topic("replyTopic")).selector("env='myenv'")
        .textMessage("hello from gatling jms dsl")
        .check(checkBodyTextCorrect))
      .exec(jms("req reply testing").reqreply
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
    .protocols(jmsConfig)

  def checkBodyTextCorrect = simpleCheck {
    case tm: TextMessage => tm.getText == "HELLO FROM GATLING JMS DSL"
    case _               => false
  }
}
