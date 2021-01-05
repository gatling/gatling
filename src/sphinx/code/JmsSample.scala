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

//#example-simulation
import javax.jms._

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.jms.Predef._

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
    exec(
      jms("req reply testing").requestReply
        .queue("jmstestq")
        .textMessage("hello from gatling jms dsl")
        .property("test_header", "test_value")
        .jmsType("test_jms_type")
        .check(simpleCheck(checkBodyTextCorrect))
    )
  }

  setUp(scn.inject(rampUsersPerSec(10).to(1000).during(2.minutes)))
    .protocols(jmsConfig)

  def checkBodyTextCorrect(m: Message) = {
    // this assumes that the service just does an "uppercase" transform on the text
    m match {
      case tm: TextMessage => tm.getText == "HELLO FROM GATLING JMS DSL"
      case _               => false
    }
  }
}
//#example-simulation

class Imports {
  //#imprts
  import javax.jms._

  import io.gatling.jms.Predef._
  //#imprts
}
