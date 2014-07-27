/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms.client

import javax.jms._

import io.gatling.jms.{ JmsTopic, JmsQueue, MockMessage }

class SimpleJmsClientSpec extends BrokerBasedSpecification with MockMessage {

  // FIXME : find a way to properly call client.close() inside JmsClient
  case class JmsClient(name: String) {
    val client = createClient(JmsQueue(name))
    val consumer = client.createReplyConsumer()
  }

  val propKey = "key"

  "simple client" should "should send  and pick up text message" in new JmsClient("text") {
    val payload = "hello message"
    val properties = Map(propKey -> name)
    val sentMsg = client.sendTextMessage(payload, properties).asInstanceOf[TextMessage]
    val receivedMsg = consumer.receive().asInstanceOf[TextMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getText shouldBe payload
    receivedMsg.getStringProperty(propKey) shouldBe name

    client.close()
  }

  it should "should send and pick up map message" in new JmsClient("map") {
    val payload = Map("msg" -> "hello message")
    val properties = Map(propKey -> name)
    val sentMsg = client.sendMapMessage(payload, properties).asInstanceOf[MapMessage]
    val receivedMsg = consumer.receive().asInstanceOf[MapMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getObject("msg") shouldBe payload("msg")
    receivedMsg.getStringProperty(propKey) shouldBe name

    client.close()
  }

  it should "should send and pick up bytes message" in new JmsClient("bytes") {
    val payload = Array[Byte](1, 2, 3)
    val properties = Map(propKey -> name)
    val sentMsg = client.sendBytesMessage(payload, properties).asInstanceOf[BytesMessage]
    val receivedMsg = consumer.receive().asInstanceOf[BytesMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getBodyLength shouldBe 3
    receivedMsg.getStringProperty(propKey) shouldBe name

    client.close()
  }

  it should "should send and pick up object message" in new JmsClient("object") {
    val payload = JmsTopic(name)
    val properties = Map(propKey -> name)
    val sentMsg = client.sendObjectMessage(payload, properties).asInstanceOf[ObjectMessage]
    val receivedMsg = consumer.receive().asInstanceOf[ObjectMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getObject shouldBe payload
    receivedMsg.getStringProperty(propKey) shouldBe name

    client.close()
  }
}
