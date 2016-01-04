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
package io.gatling.jms.client

import javax.jms._

import io.gatling.jms.request.{ JmsTopic, JmsQueue }
import io.gatling.jms.MockMessage

class SimpleJmsClientSpec extends BrokerBasedSpec with MockMessage {

  def withJmsClient(name: String)(testCode: (SimpleJmsClient, MessageConsumer, String) => Any): Unit = {
    val client = createClient(JmsQueue(name))
    val consumer = client.createReplyConsumer()
    try {
      testCode(client, consumer, name)
    } finally {
      client.close()
    }
  }

  val propKey = "key"

  "simple client" should "send  and pick up text message" in withJmsClient("text") { (client, consumer, name) =>
    val payload = "hello message"
    val properties = Map(propKey -> name)
    val sentMsg = client.sendTextMessage(payload, properties).asInstanceOf[TextMessage]
    val receivedMsg = consumer.receive().asInstanceOf[TextMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getText shouldBe payload
    receivedMsg.getStringProperty(propKey) shouldBe name
  }

  it should "send and pick up map message" in withJmsClient("map") { (client, consumer, name) =>
    val payload = Map("msg" -> "hello message")
    val properties = Map(propKey -> name)
    val sentMsg = client.sendMapMessage(payload, properties).asInstanceOf[MapMessage]
    val receivedMsg = consumer.receive().asInstanceOf[MapMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getObject("msg") shouldBe payload("msg")
    receivedMsg.getStringProperty(propKey) shouldBe name
  }

  it should "send and pick up bytes message" in withJmsClient("bytes") { (client, consumer, name) =>
    val payload = Array[Byte](1, 2, 3)
    val properties = Map(propKey -> name)
    val sentMsg = client.sendBytesMessage(payload, properties).asInstanceOf[BytesMessage]
    val receivedMsg = consumer.receive().asInstanceOf[BytesMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getBodyLength shouldBe 3
    receivedMsg.getStringProperty(propKey) shouldBe name
  }

  it should "send and pick up object message" in withJmsClient("object") { (client, consumer, name) =>
    val payload = JmsTopic(name)
    val properties = Map(propKey -> name)
    val sentMsg = client.sendObjectMessage(payload, properties).asInstanceOf[ObjectMessage]
    val receivedMsg = consumer.receive().asInstanceOf[ObjectMessage]

    receivedMsg shouldBe sentMsg
    receivedMsg.getObject shouldBe payload
    receivedMsg.getStringProperty(propKey) shouldBe name
  }
}
