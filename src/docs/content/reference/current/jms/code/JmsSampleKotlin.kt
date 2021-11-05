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

//#imprts
import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.jms.*
import io.gatling.javaapi.jms.JmsDsl.*
import javax.jms.*
//#imprts
import org.apache.activemq.ActiveMQConnectionFactory

class JmsSampleKotlin {

init {
//#jndi
val jndiBasedConnectionFactory = jmsJndiConnectionFactory
  .connectionFactoryName("ConnectionFactory")
  .url("url")
  .credentials("username", "password")
  .contextFactory("ContextFactoryClassName")

val jmsProtocol: JmsProtocolBuilder = jms
  .connectionFactory(jndiBasedConnectionFactory)
//#jndi
}
init {
//#prog
val connectionFactory: ConnectionFactory = ActiveMQConnectionFactory("url")

val jmsProtocol = jms
  .connectionFactory(connectionFactory)
//#prog

//#options
jms
  .connectionFactory(connectionFactory)
  .credentials("username", "password") // optional, default to non persistent
  .useNonPersistentDeliveryMode()
  .useNonPersistentDeliveryMode() // optional, default to 1
  // listener thread count
  // some JMS implementation (like IBM MQ) need more than one MessageListener
  // to achieve full readout performance
  .listenerThreadCount(5) // optional, default to `matchByMessageId`
  // specify how request and response messages should be matched when using `requestReply`
  // Use `matchByCorrelationId` for ActiveMQ.
  .matchByCorrelationId()
  .matchByMessageId() // use a custom matching strategy
  // see io.gatling.javaapi.jms.JmsMessageMatcher
  .messageMatcher(null as io.gatling.javaapi.jms.JmsMessageMatcher) // optional, default to none
  // reply timeout
  .replyTimeout(1000)
//#options
}

init {
//#message
// with a static text message
jms("name").send().queue("queueName")
  .textMessage("message")
// with a Gatling EL string text message
jms("name").send().queue("queueName")
  .textMessage("#{message}")
// with a function text message
jms("name").send().queue("queueName")
  .textMessage { session -> session.getString("message") }
// with a ElFileBody template text message
jms("name").send().queue("queueName")
  .textMessage(ElFileBody("templatePath"))

// with a static bytes message
jms("name").send().queue("queueName")
  .bytesMessage(byteArrayOf(0, 1, 2))
// with a RawFileBody bytes message
jms("name").send().queue("queueName")
  .bytesMessage(RawFileBody("templatePath"))
//#message

//#extra
jms("name").send().queue("queueName")
  .textMessage("message")
  .jmsType("type")
  .property("foo", "bar")
//#extra
}

//#simple
fun checkBodyTextCorrect(m: Message?): Boolean {
  // this assumes that the service just does an "uppercase" transform on the text
  return if (m is TextMessage) {
      m.text == "HELLO FROM GATLING JMS DSL"
  } else {
    false
  }
}

val request = jms("name").requestReply().queue("queueName")
.textMessage("message")
.check(JmsDsl.simpleCheck { m: Message? -> checkBodyTextCorrect(m) })
//#simple

//#example-simulation
class TestJmsDsl : Simulation() {
  // create a ConnectionFactory for ActiveMQ
  // search the documentation of your JMS broker
  val connectionFactory: ConnectionFactory = ActiveMQConnectionFactory("tcp://localhost:61616")

  // alternatively, you can create a ConnectionFactory from a JNDI lookup
  val jndiBasedConnectionFactory = jmsJndiConnectionFactory
    .connectionFactoryName("ConnectionFactory")
    .url("tcp://localhost:61616")
    .credentials("user", "secret")
    .contextFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory")
  val jmsProtocol = jms
    .connectionFactory(connectionFactory)
    .usePersistentDeliveryMode()
  val scn = scenario("JMS DSL test").repeat(1).on(
    exec(jms("req reply testing").requestReply()
      .queue("jmstestq")
      .textMessage("hello from gatling jms dsl")
      .property("test_header", "test_value")
      .jmsType("test_jms_type")
      .check(xpath("//foo")))
  )

  init {
    setUp(scn.injectOpen(rampUsersPerSec(10.0).to(1000.0).during(60)))
      .protocols(jmsProtocol)
  }
}
//#example-simulation
}
