package io.gatling.jms.integration

import javax.jms.{ Message, MessageListener }

import akka.actor.ActorRef
import akka.testkit.{ ImplicitSender, TestKit }
import io.gatling.core.config.{ GatlingConfiguration, Protocols }
import io.gatling.core.pause.Constant
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session.Session
import io.gatling.core.structure.{ ScenarioContext, ScenarioBuilder }
import io.gatling.jms.JmsDestination
import io.gatling.jms._
import io.gatling.jms.client.{ SimpleJmsClient, BrokerBasedSpecification }
import org.apache.activemq.jndi.ActiveMQInitialContextFactory
import org.scalatest.mock.MockitoSugar

import scala.concurrent.duration._

class JmsMockCustomer(client: SimpleJmsClient, mockResponse: PartialFunction[Message, String]) extends MessageListener {

  val producer = client.session.createProducer(null)
  client.createReplyConsumer().setMessageListener(this)

  override def onMessage(request: Message): Unit = {
    if (mockResponse.isDefinedAt(request)) {
      val response = client.session.createTextMessage(mockResponse(request))
      response.setJMSCorrelationID(request.getJMSMessageID)
      producer.send(request.getJMSReplyTo, response)
    }
  }

  def close(): Unit = {
    producer.close()
    client.close()
  }
}

trait JmsMockingSpec extends BrokerBasedSpecification with MockitoSugar with JmsModule {

  def jmsProtocol = jms
    .connectionFactoryName("ConnectionFactory")
    .url("vm://gatling?broker.persistent=false&broker.useJmx=false")
    .contextFactory(classOf[ActiveMQInitialContextFactory].getName)
    .listenerCount(1)

  def runScenario(sb: ScenarioBuilder, timeout: FiniteDuration = 10.seconds, protocols: Protocols = Protocols(jmsProtocol))(implicit testKit: TestKit with ImplicitSender, configuration: GatlingConfiguration) = {
    import testKit._

    val actor = sb.build(self, ScenarioContext(mock[ActorRef], mock[DataWriters], mock[ActorRef], protocols, Constant, throttled = false))
    actor ! Session("TestSession", "testUser")
    val session = expectMsgClass(timeout, classOf[Session])

    session
  }

  def jmsMock(queue: JmsDestination, f: PartialFunction[Message, String]) = {
    val processor = new JmsMockCustomer(createClient(queue), f)
    cleanUpActions = { () => processor.close() } :: cleanUpActions
  }
}
