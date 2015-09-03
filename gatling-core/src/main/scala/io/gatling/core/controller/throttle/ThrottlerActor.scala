/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.core.controller.throttle

import java.lang.System._

import scala.concurrent.duration._
import scala.collection.mutable

case class ThrottledRequest(scenarioName: String, request: () => Unit)

class ThrottlerActor extends ThrottlerFSM {

  import ThrottlerState._
  import ThrottlerData._

  // FIXME use a capped size? or kill when overflow?

  startWith(WaitingToStart, NoData)

  when(WaitingToStart) {

    case Event(throttles: Throttles, NoData) =>
      goto(Started) using StartedData(throttles, mutable.ArrayBuffer.empty[(String, () => Unit)], nanoTime)
  }

  def millisSinceTick(tickNanos: Long): Int = ((nanoTime - tickNanos) / 1000000).toInt

  private def sendRequest(data: StartedData, request: () => Unit): Unit = {
    import data._
    if (count == 0) {
      request()
    } else {
      val delay = (requestStep * count).toInt - millisSinceTick(tickNanos)
      scheduler.scheduleOnce(delay milliseconds) {
        request()
      }
    }
  }

  private def sendOrEnqueueRequest(data: StartedData, scenarioName: String, request: () => Unit): Unit = {
    import data._
    if (throttles.limitReached(scenarioName)) {
      buffer += (scenarioName -> request)

    } else {
      sendRequest(data, request)
      throttles.global.foreach(_.increment())
      throttles.perScenario.get(scenarioName).foreach(_.increment())
      data.incrementCount()
    }
  }

  when(Started) {
    case Event(throttles: Throttles, data: StartedData) =>

      val newData = new StartedData(throttles, new mutable.ArrayBuffer[(String, () => Unit)](data.buffer.size), nanoTime)

      data.buffer.foreach {
        case (scenarioName, request) =>
          sendOrEnqueueRequest(newData, scenarioName, request)
      }

      stay() using newData

    case Event(ThrottledRequest(scenarioName, request), data: StartedData) =>
      sendOrEnqueueRequest(data, scenarioName, request)
      stay()
  }
}
