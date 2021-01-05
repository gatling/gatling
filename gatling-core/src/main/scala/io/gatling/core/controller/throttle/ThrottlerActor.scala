/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import scala.collection.mutable
import scala.concurrent.duration._

final case class ThrottledRequest(scenarioName: String, request: () => Unit)

class ThrottlerActor extends ThrottlerActorFSM {

  import ThrottlerActorData._
  import ThrottlerActorState._

  startWith(WaitingToStart, NoData)

  when(WaitingToStart) { case Event(throttles: Throttles, NoData) =>
    // FIXME use a capped size? or kill when overflow?
    goto(Started) using StartedData(throttles, mutable.ArrayBuffer.empty[ThrottledRequest], nanoTime)
  }

  private def millisSinceTick(tickNanos: Long): Int = ((nanoTime - tickNanos) / 1000000).toInt

  private def sendRequest(data: StartedData, request: () => Unit): Unit = {
    import data._
    if (count == 0) {
      request()
    } else {
      val delay = (requestStep * count).toInt - millisSinceTick(tickNanos)
      scheduler.scheduleOnce(delay.milliseconds) {
        request()
      }
    }
  }

  private def sendOrEnqueueRequest(data: StartedData, throttledRequest: ThrottledRequest): Unit = {
    import data._
    if (throttles.limitReached(throttledRequest.scenarioName)) {
      buffer += throttledRequest

    } else {
      sendRequest(data, throttledRequest.request)
      throttles.increment(throttledRequest.scenarioName)
      data.incrementCount()
    }
  }

  when(Started) {
    case Event(throttles: Throttles, data: StartedData) =>
      val newData = StartedData(throttles, new mutable.ArrayBuffer[ThrottledRequest](data.buffer.size), nanoTime)
      data.buffer.foreach(sendOrEnqueueRequest(newData, _))
      stay() using newData

    case Event(throttledRequest: ThrottledRequest, data: StartedData) =>
      sendOrEnqueueRequest(data, throttledRequest)
      stay()
  }
}
