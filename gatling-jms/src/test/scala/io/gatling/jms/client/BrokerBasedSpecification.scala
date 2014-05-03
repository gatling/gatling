package io.gatling.jms.client

import org.specs2.mutable.Specification
import org.specs2.specification.{ Step, Fragments }
import org.apache.activemq.broker.{ BrokerFactory, BrokerService }
import io.gatling.jms.JmsDestination
import org.apache.activemq.jndi.ActiveMQInitialContextFactory

trait BrokerBasedSpecification extends Specification {

  /** the map method allows to "post-process" the fragments after their creation */
  override def map(fs: => Fragments) = Step(startBroker()) ^ fs ^ Step(stopBroker())

  lazy val broker: BrokerService = BrokerFactory.createBroker("broker://()/gatling?persistent=false&useJmx=false")

  def startBroker() = {
    {
      broker.start()
      broker.waitUntilStarted()
    }
  }

  def stopBroker() = {
    broker.stop()
    broker.waitUntilStopped()
  }

  def createClient(destination: JmsDestination) = {
    new SimpleJmsClient("ConnectionFactory",
      destination, destination,
      "vm://gatling?broker.persistent=false&broker.useJmx=false", None,
      classOf[ActiveMQInitialContextFactory].getName, 1)
  }

}
