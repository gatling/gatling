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

package io.gatling.core.controller.inject.open

import scala.concurrent.duration._

import io.gatling.commons.util.PushbackIterator

private[inject] object UserStreamBatchResult {
  val Empty = UserStreamBatchResult(0, continue = false)
}

private[inject] final case class UserStreamBatchResult(count: Long, continue: Boolean)

object UserStream {
  def apply(steps: Iterable[OpenInjectionStep]): UserStream = {
    val users = steps.foldRight(Iterator.empty: Iterator[FiniteDuration]) { (step, iterator) =>
      step.chain(iterator)
    }
    new UserStream(users)
  }
}

private[inject] class UserStream(users: Iterator[FiniteDuration]) {

  private val stream: PushbackIterator[FiniteDuration] = new PushbackIterator(users)

  def withStream(batchWindow: FiniteDuration, injectTime: Long, startTime: Long)(f: FiniteDuration => Unit): UserStreamBatchResult =
    if (stream.hasNext) {
      val batchTimeOffset = (injectTime - startTime).millis
      val nextBatchTimeOffset = batchTimeOffset + batchWindow

      var continue = true
      var streamNonEmpty = true
      var count = 0L

      while (streamNonEmpty && continue) {
        val startingTime = stream.next()
        streamNonEmpty = stream.hasNext
        val delay = startingTime - batchTimeOffset
        continue = startingTime < nextBatchTimeOffset

        if (continue) {
          count += 1
          f(delay)
        } else {
          streamNonEmpty = true
          stream.pushback(startingTime)
        }
      }

      UserStreamBatchResult(count, streamNonEmpty)
    } else {
      UserStreamBatchResult.Empty
    }
}
