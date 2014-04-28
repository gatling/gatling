package io.gatling.core.result.writer

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
import java.lang.System.currentTimeMillis

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

import io.gatling.core.result.message.{ End, Start }

class LeakReporterDataWriter extends DataWriter {

  private var lastTouch = 0L
  private val events = mutable.Map.empty[String, DataWriterMessage]

  def display() {
    val timeSinceLastTouch = (currentTimeMillis - lastTouch) / 1000

    if (timeSinceLastTouch > 30 && !events.isEmpty) {
      System.err.println("Gatling had no activity during last 30s. It could be a virtual user leak, here's their last events:")
      events.values.foreach(System.err.println)
    }
  }

  override def initialized: Receive = super.initialized.orElse {
    case Display => display()
  }

  override def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription]) {

    lastTouch = currentTimeMillis

    scheduler.schedule(0 seconds, 30 seconds, self, Display)
  }

  override def onUserMessage(userMessage: UserMessage) {

    lastTouch = currentTimeMillis

    userMessage.event match {
      case Start => events += userMessage.userId -> userMessage
      case End   => events -= userMessage.userId
    }
  }

  override def onGroupMessage(groupMessage: GroupMessage) {

    lastTouch = currentTimeMillis
    events += groupMessage.userId -> groupMessage
  }

  override def onRequestMessage(requestMessage: RequestMessage) {

    lastTouch = currentTimeMillis
    events += requestMessage.userId -> requestMessage
  }

  override def onTerminateDataWriter() {}
}
