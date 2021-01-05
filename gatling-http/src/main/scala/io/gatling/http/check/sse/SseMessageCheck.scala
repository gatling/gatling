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

package io.gatling.http.check.sse

import scala.concurrent.duration.FiniteDuration

import com.softwaremill.quicklens._

final case class SseMessageCheckSequence(timeout: FiniteDuration, checks: List[SseMessageCheck]) {
  require(checks.nonEmpty, "Can't pass empty check sequence")
}

final case class SseMessageCheck(name: String, matchConditions: List[SseCheck], checks: List[SseCheck]) {

  def matching(newMatchConditions: SseCheck*): SseMessageCheck =
    this.modify(_.matchConditions).using(_ ::: newMatchConditions.toList)

  def check(newChecks: SseCheck*): SseMessageCheck =
    this.modify(_.checks).using(_ ::: newChecks.toList)
}
