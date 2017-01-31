/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.jms.{ Destination, MessageProducer }

import scala.collection.JavaConverters._

class JmsProducerPool(sessionPool: JmsSessionPool) {

  private val registeredProducers = Collections.newSetFromMap(new ConcurrentHashMap[MessageProducer, java.lang.Boolean])
  private val producers = new ConcurrentHashMap[(Destination, Int), ThreadLocal[JmsProducer]]

  def producer(destination: Destination, deliveryMode: Int): ThreadLocal[JmsProducer] =
    producers.computeIfAbsent((destination, deliveryMode), _ => ThreadLocal.withInitial[JmsProducer](() => {
      val jmsSession = sessionPool.jmsSession()
      val producer = jmsSession.createProducer(destination)
      producer.setDeliveryMode(deliveryMode)
      registeredProducers.add(producer)
      new JmsProducer(jmsSession, producer)
    }))

  def close(): Unit = registeredProducers.asScala.foreach(_.close())
}
