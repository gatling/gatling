/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
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

class JMSCompileTest extends Simulation {

  val jmsConfig = jms
    .connectionFactoryName("FFMQConstants.JNDI_CONNECTION_FACTORY_NAME")
    .url("tcp://localhost:10002")
    .credentials("user", "secret")
    .contextFactory("FFMQConstants.JNDI_CONTEXT_FACTORY")
    .listenerCount(1)
    .usePersistentDeliveryMode

  val scn = scenario("JMS DSL test").repeat(1) {
    exec(jms("req reply testing").reqreply
      .queue("jmstestq")
      // -- four message types are supported; only StreamMessage is not currently supported
      .textMessage("hello from gatling jms dsl")
      .property("test_header", "test_value")
      .check(checkBodyTextCorrect))
    exec(jms("req reply testing").reqreply
      .queue("jmstestq")
      .bytesMessage(new Array[Byte](1))
      .property("test_header", "test_value")
      .check(checkBodyTextCorrect))
    exec(jms("req reply testing").reqreply
      .queue("jmstestq")
      .mapMessage(Map("foo" -> "bar"))
      .property("test_header", "test_value")
      .check(checkBodyTextCorrect))
    exec(jms("req reply testing").reqreply
      .queue("jmstestq")
      .objectMessage("hello!")
      .property("test_header", "test_value")
      .check(checkBodyTextCorrect))
  }

  setUp(scn.inject(rampUsersPerSec(10) to (1000) during (2 minutes)))
    .protocols(jmsConfig)

  def checkBodyTextCorrect(m: Message) = {
    // this assumes that the service just does an "uppercase" transform on the text
    m match {
      case tm: TextMessage => tm.getText.toString == "HELLO FROM GATLING JMS DSL"
      case _               => false
    }
  }
}
