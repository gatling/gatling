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
package io.gatling.metrics.sender

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.util.TimeHelper.nowMillis

private[sender] class Failures private (maxFailuresLimit: Int, failureWindow: FiniteDuration, failures: List[Long]) {

  def this(maxFailuresLimit: Int, failureWindow: FiniteDuration) =
    this(maxFailuresLimit, failureWindow, Nil)

  private def copyWithNewFailures(failures: List[Long]) =
    new Failures(maxFailuresLimit, failureWindow, failures)

  def newFailure: Failures = copyWithNewFailures(nowMillis :: cleanupOldFailures)

  def isLimitReached = cleanupOldFailures.length >= maxFailuresLimit

  private def cleanupOldFailures: List[Long] =
    failures.filterNot(_ < (nowMillis - failureWindow.toMillis))
}
