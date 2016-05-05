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
//#imports
import io.gatling.jms.Predef._
import javax.jms._
//#imports

//#example-simulation
import net.timewalker.ffmq3.FFMQConstants
import io.gatling.core.Predef._
import io.gatling.jms.Predef._
import javax.jms._
import scala.concurrent.duration._

class TestJmsDsl extends Simulation {

  val jmsConfig = jms
    .connectionFactoryName(FFMQConstants.JNDI_CONNECTION_FACTORY_NAME)
    .url("tcp://localhost:10002")
    .credentials("user", "secret")
    .contextFactory(FFMQConstants.JNDI_CONTEXT_FACTORY)
    .listenerCount(1)
    .usePersistentDeliveryMode

  val scn = scenario("JMS DSL test").repeat(1) {
    exec(jms("req reply testing").reqreply
      .queue("jmstestq")
      .textMessage("hello from gatling jms dsl")
      .property("test_header", "test_value")
      .jmsType("test_jms_type")
      .check(simpleCheck(checkBodyTextCorrect))
    )
  }

  setUp(scn.inject(rampUsersPerSec(10) to 1000 during (2 minutes)))
    .protocols(jmsConfig)

  def checkBodyTextCorrect(m: Message) = {
    // this assumes that the service just does an "uppercase" transform on the text
    m match {
      case tm: TextMessage => tm.getText == "HELLO FROM GATLING JMS DSL"
      case _ => false
    }
  }
}
//#example-simulation
