package io.gatling.jms.client

import io.gatling.jms.{ JmsTopic, JmsQueue, MockMessage }
import javax.jms._
import org.specs2.specification.After

//import org.specs2.mutable.After

class SimpleJmsClientSpec extends BrokerBasedSpecification with MockMessage {

  case class JmsClient(name: String) extends After {
    val client = createClient(JmsQueue(name))
    val consumer = client.createReplyConsumer()

    def after = {
      client.close()
    }
  }

  "simple client" should {
    val key = "key"

    "should send  and pick up text message" in new JmsClient("text") {
      val payload = "hello message"
      val properties = Map(key -> name)
      val sentMsg = client.sendTextMessage(payload, properties).asInstanceOf[TextMessage]
      val receivedMsg = consumer.receive().asInstanceOf[TextMessage]

      receivedMsg must_== sentMsg
      receivedMsg.getText must_== payload
      receivedMsg.getStringProperty(key) must_== name
    }

    "should send and pick up map message" in new JmsClient("map") {
      val payload = Map("msg" -> "hello message")
      val properties = Map(key -> name)
      val sentMsg = client.sendMapMessage(payload, properties).asInstanceOf[MapMessage]
      val receivedMsg = consumer.receive().asInstanceOf[MapMessage]

      receivedMsg must_== sentMsg
      receivedMsg.getObject("msg") must_== payload("msg")
      receivedMsg.getStringProperty(key) must_== name
    }

    "should send and pick up bytes message" in new JmsClient("bytes") {
      val payload = Array[Byte](1, 2, 3)
      val properties = Map(key -> name)
      val sentMsg = client.sendBytesMessage(payload, properties).asInstanceOf[BytesMessage]
      val receivedMsg = consumer.receive().asInstanceOf[BytesMessage]

      receivedMsg must_== sentMsg
      receivedMsg.getBodyLength must_== 3
      receivedMsg.getStringProperty(key) must_== name
    }

    "should send and pick up object message" in new JmsClient("object") {
      val payload = JmsTopic(name)
      val properties = Map(key -> name)
      val sentMsg = client.sendObjectMessage(payload, properties).asInstanceOf[ObjectMessage]
      val receivedMsg = consumer.receive().asInstanceOf[ObjectMessage]

      receivedMsg must_== sentMsg
      receivedMsg.getObject must_== payload
      receivedMsg.getStringProperty(key) must_== name
    }
  }
}
