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
import io.gatling.core.Predef._
//#imprts
import javax.jms._
import io.gatling.jms.Predef._
//#imprts

class ProtocolSamples {

  {
//#jndi
val jndiBasedConnectionFactory = jmsJndiConnectionFactory
  .connectionFactoryName("ConnectionFactory")
  .url("tcp://localhost:61616")
  // optional, for performing JNDI lookup
  .credentials("user", "secret")
  // optional, custom JNDI property
  .property("foo", "bar")
  .contextFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory")

val jmsProtocol = jms
  .connectionFactory(jndiBasedConnectionFactory)
//#jndi
  }

  {
//#prog
val connectionFactory =
  new org.apache.activemq.ActiveMQConnectionFactory("url")

val jmsProtocol = jms
  .connectionFactory(connectionFactory)
//#prog
//#options
jms
  .connectionFactory(connectionFactory)
  .credentials("username", "password")
  // optional, default to non persistent
  .useNonPersistentDeliveryMode
  .useNonPersistentDeliveryMode

  // optional, default to 1
  // listener thread count
  // some JMS implementation (like IBM MQ) need more than one MessageListener
  // to achieve full readout performance
  .listenerThreadCount(5)

  // optional, default to `matchByMessageId`
  // specify how request and response messages should be matched
  // Use `matchByCorrelationId` for ActiveMQ.
  .matchByCorrelationId
  .matchByMessageId
  // use a custom matching strategy
  .messageMatcher(null.asInstanceOf[io.gatling.jms.protocol.JmsMessageMatcher])

  // optional, default to none
  // reply timeout
  .replyTimeout(1000)
//#options
  }

  {
//#message
// with a static text message
jms("name").send.queue("queueName")
  .textMessage("message")
// with a Gatling EL string text message
jms("name").send.queue("queueName")
  .textMessage("#{message}")
// with a function text message
jms("name").send.queue("queueName")
  .textMessage(session => session("message").as[String])
// with a ElFileBody template text message
jms("name").send.queue("queueName")
  .textMessage(ElFileBody("templatePath"))

// with a static bytes message
jms("name").send.queue("queueName")
  .bytesMessage(Array[Byte](0, 1, 2))
// with a RawFileBody bytes message
jms("name").send.queue("queueName")
  .bytesMessage(RawFileBody("templatePath"))
//#message

//#extra
jms("name").send.queue("queueName")
  .textMessage("message")
  .jmsType("type")
  .property("foo", "bar")
//#extra
  }

//#simple
def checkBodyTextCorrect(m: Message) = {
  // this assumes that the service just does an "uppercase" transform on the text
  m match {
    case tm: TextMessage => tm.getText == "HELLO FROM GATLING JMS DSL"
    case _               => false
  }
}

val request =
  jms("name").requestReply.queue("queueName")
    .textMessage("message")
    .check(simpleCheck(checkBodyTextCorrect))
//#simple
}

//#example-simulation
class TestJmsDsl extends Simulation {

  // create a ConnectionFactory for ActiveMQ
  // search the documentation of your JMS broker
  val connectionFactory =
    new org.apache.activemq.ActiveMQConnectionFactory("tcp://localhost:61616")

  // alternatively, you can create a ConnectionFactory from a JNDI lookup
  val jndiBasedConnectionFactory = jmsJndiConnectionFactory
    .connectionFactoryName("ConnectionFactory")
    .url("tcp://localhost:61616")
    .credentials("user", "secret")
    .contextFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory")

  val jmsConfig = jms
    .connectionFactory(connectionFactory)
    .usePersistentDeliveryMode

  val scn = scenario("JMS DSL test").repeat(1) {
    exec(jms("req reply testing").requestReply
      .queue("jmstestq")
      .textMessage("hello from gatling jms dsl")
      .property("test_header", "test_value")
      .jmsType("test_jms_type")
      .check(xpath("//foo")))
  }

  setUp(scn.inject(rampUsersPerSec(10).to(1000).during(60)))
    .protocols(jmsConfig)
}
//#example-simulation
