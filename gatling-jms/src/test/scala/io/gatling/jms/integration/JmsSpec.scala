/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import scala.concurrent.duration._

import io.gatling.commons.util.DefaultClock
import io.gatling.core.{ CoreComponents, EmptySession }
import io.gatling.core.action.ActorDelegatingAction
import io.gatling.core.actor.ActorSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.pause.Constant
import io.gatling.core.protocol.{ Protocol, ProtocolComponentsRegistries, Protocols }
import io.gatling.core.session.{ Session, StaticValueExpression }
import io.gatling.core.stats.NoopStatsEngine
import io.gatling.core.structure.{ ScenarioBuilder, ScenarioContext }
import io.gatling.jms._
import io.gatling.jms.protocol.JmsProtocolBuilder
import io.gatling.jms.request._

import com.github.marschall.jakartajmsadapter.JakartaConnectionFactory
import jakarta.jms.{ Session => JmsSession, _ }
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.broker.{ BrokerFactory, BrokerService }

class Replier(connectionFactory: ConnectionFactory, destination: JmsDestination, response: PartialFunction[(Message, JmsSession), Message]) {
  private val t = new Thread(() => {
    val connection = connectionFactory.createConnection()
    val jmsSession = connection.createSession(false, JmsSession.AUTO_ACKNOWLEDGE)
    val consumedDestination: Destination =
      destination match {
        case JmsDestination.TemporaryQueue                     => jmsSession.createTemporaryQueue()
        case JmsDestination.TemporaryTopic                     => jmsSession.createTemporaryTopic()
        case JmsDestination.Queue(StaticValueExpression(name)) => jmsSession.createQueue(name)
        case JmsDestination.Topic(StaticValueExpression(name)) => jmsSession.createTopic(name)
        case _                                                 => throw new UnsupportedOperationException("Support not implemented in this test yet")
      }

    val consumer = jmsSession.createConsumer(consumedDestination)
    val producer = jmsSession.createProducer(null)
    consumer.setMessageListener { request =>
      response.lift(request, jmsSession).foreach { response =>
        response.setJMSCorrelationID(request.getJMSCorrelationID)
        producer.send(request.getJMSReplyTo, response)
      }
    }
    connection.start()
  })

  t.start()
}

trait JmsSpec extends ActorSpec with JmsDsl with EmptySession {
  override def beforeAll(): Unit = {
    sys.props += "org.apache.activemq.SERIALIZABLE_PACKAGES" -> "io.gatling"
    startBroker()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    synchronized {
      cleanUpActions.foreach(_.apply())
      cleanUpActions = Nil
    }
    stopBroker()
  }

  var cleanUpActions: List[() => Unit] = Nil

  protected def registerCleanUpAction(f: () => Unit): Unit = synchronized {
    cleanUpActions = f :: cleanUpActions
  }

  lazy val broker: BrokerService = BrokerFactory.createBroker("broker://()/gatling?persistent=false&useJmx=false")

  def startBroker(): Boolean = {
    broker.start()
    broker.waitUntilStarted()
  }

  def stopBroker(): Unit = {
    broker.stop()
    broker.waitUntilStopped()
  }

  val cf = new JakartaConnectionFactory(new ActiveMQConnectionFactory("vm://gatling?broker.persistent=false&broker.useJmx=false"))

  implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  def jmsProtocol: JmsProtocolBuilder =
    jms
      .connectionFactory(cf)
      .matchByCorrelationId

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def runScenario(sb: ScenarioBuilder, timeout: FiniteDuration = 10.seconds, protocols: Protocols = Protocol.indexByType(Seq(jmsProtocol)))(implicit
      configuration: GatlingConfiguration
  ): Session = {
    val clock = new DefaultClock
    val coreComponents =
      new CoreComponents(actorSystem, null, null, None, NoopStatsEngine, clock, null, configuration)
    val nextActor = mockActorRef[Session]("next")
    val next = new ActorDelegatingAction("next", nextActor)
    val protocolComponentsRegistry = new ProtocolComponentsRegistries(coreComponents, protocols).scenarioRegistry(Map.empty)
    val actor = sb.build(new ScenarioContext(coreComponents, protocolComponentsRegistry, Constant, throttled = false), next)
    actor ! emptySession
    val session = nextActor.expectMsgType[Session](timeout)

    session
  }

  def replier(queue: JmsDestination, f: PartialFunction[(Message, JmsSession), Message]): Replier =
    new Replier(cf, queue, f)
}
