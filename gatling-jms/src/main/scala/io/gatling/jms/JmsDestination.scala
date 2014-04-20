package io.gatling.jms

sealed trait JmsDestination
case class JmsQueue(name: String) extends JmsDestination
case class JmsTopic(name: String) extends JmsDestination
case object JmsTemporaryQueue extends JmsDestination
case object JmsTemporaryTopic extends JmsDestination