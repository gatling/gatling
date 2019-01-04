/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.util

import scala.concurrent.duration.FiniteDuration

private[gatling] class Retry private (maxRetryLimit: Int, retryWindow: FiniteDuration, retries: List[Long], clock: Clock) {

  def this(maxRetryLimit: Int, retryWindow: FiniteDuration, clock: Clock) =
    this(maxRetryLimit, retryWindow, Nil, clock)

  private def copyWithNewRetries(retries: List[Long]) =
    new Retry(maxRetryLimit, retryWindow, retries, clock)

  def newRetry: Retry = copyWithNewRetries(clock.nowMillis :: cleanupOldRetries)

  def isLimitReached = cleanupOldRetries.length >= maxRetryLimit

  private def cleanupOldRetries: List[Long] = {
    val now = clock.nowMillis
    retries.filterNot(_ < (now - retryWindow.toMillis))
  }
}
