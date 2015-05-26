/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.stats.writer

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
import scala.concurrent.duration.{ FiniteDuration, DurationInt }

import io.gatling.core.stats.message.{ End, Start }

class LeakData(val noActivityTimeout: FiniteDuration, var lastTouch: Long, val events: mutable.Map[Long, DataWriterMessage]) extends DataWriterData

class LeakReporterDataWriter extends DataWriter[LeakData] {

  private val flushTimerName = "flushTimer"

  def onInit(init: Init): LeakData = {
    import init._

    val noActivityTimeout = configuration.data.leak.noActivityTimeout seconds

    setTimer(flushTimerName, Flush, noActivityTimeout, repeat = true)

    new LeakData(noActivityTimeout, currentTimeMillis, mutable.Map.empty[Long, DataWriterMessage])
  }

  override def onFlush(data: LeakData): Unit = {
    import data._
    val timeSinceLastTouch = (currentTimeMillis - lastTouch) / 1000

    if (timeSinceLastTouch > noActivityTimeout.toSeconds && events.nonEmpty) {
      System.err.println(s"Gatling had no activity during last $noActivityTimeout. It could be a virtual user leak, here's their last events:")
      events.values.foreach(System.err.println)
    }
  }

  private def onUserMessage(user: UserMessage, data: LeakData): Unit = {
    import data._
    lastTouch = currentTimeMillis
    user.event match {
      case Start => events += user.session.userId -> user
      case End   => events -= user.session.userId
    }
  }

  private def onGroupMessage(group: GroupMessage, data: LeakData): Unit = {
    import data._
    lastTouch = currentTimeMillis
    events += group.userId -> group
  }

  private def onRequestMessage(request: RequestMessage, data: LeakData): Unit = {
    import data._
    lastTouch = currentTimeMillis
    events += request.userId -> request
  }

  private def onResponseMessage(response: ResponseMessage, data: LeakData): Unit = {
    import data._
    lastTouch = currentTimeMillis
    events += response.userId -> response
  }

  override def onMessage(message: LoadEventMessage, data: LeakData): Unit = message match {
    case user: UserMessage        => onUserMessage(user, data)
    case group: GroupMessage      => onGroupMessage(group, data)
    case request: RequestMessage  => onRequestMessage(request, data)
    case request: ResponseMessage => onResponseMessage(request, data)
    case error: ErrorMessage      =>
  }

  override def onCrash(cause: String, data: LeakData): Unit = {}

  override def onTerminate(data: LeakData): Unit = cancelTimer(flushTimerName)
}
