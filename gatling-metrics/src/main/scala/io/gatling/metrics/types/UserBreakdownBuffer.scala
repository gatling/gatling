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
package io.gatling.metrics.types

import io.gatling.core.stats.message.{ End, Start }
import io.gatling.core.stats.writer.UserMessage

private[metrics] class UserBreakdownBuffer(val totalUserEstimate: Int) {

  private var previousActive = 0
  private var previousEnd = 0
  private var thisStart = 0
  private var thisEnd = 0

  private var start = 0
  private var end = 0
  private var waiting = totalUserEstimate

  def add(userMessage: UserMessage): Unit = userMessage.event match {
    case Start =>
      start += 1
      thisStart += 1
      waiting -= 1

    case End =>
      end += 1
      thisEnd += 1
  }

  def breakDown: UserBreakdown = {

    previousActive += thisStart - previousEnd
    previousEnd = thisEnd
    thisStart = 0
    thisEnd = 0

    UserBreakdown(previousActive, math.max(waiting, 0), end)
  }
}

private[metrics] case class UserBreakdown(active: Int, waiting: Int, done: Int)
