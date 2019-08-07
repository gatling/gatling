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

package io.gatling.http.check.ws

import scala.concurrent.duration.FiniteDuration

import com.softwaremill.quicklens._

final case class WsFrameCheckSequence[+T <: WsFrameCheck](timeout: FiniteDuration, checks: List[T]) {
  require(checks.nonEmpty, "Can't pass empty check sequence")
}

sealed trait WsFrameCheck {
  def name: String
}

final case class WsBinaryFrameCheck(name: String, matchConditions: List[WsBinaryCheck], checks: List[WsBinaryCheck]) extends WsFrameCheck {

  def matching(newMatchConditions: WsBinaryCheck*): WsBinaryFrameCheck =
    this.modify(_.matchConditions).using(_ ::: newMatchConditions.toList)

  def check(newChecks: WsBinaryCheck*): WsBinaryFrameCheck =
    this.modify(_.checks).using(_ ::: newChecks.toList)
}

final case class WsTextFrameCheck(name: String, matchConditions: List[WsTextCheck], checks: List[WsTextCheck]) extends WsFrameCheck {

  def matching(newMatchConditions: WsTextCheck*): WsTextFrameCheck =
    this.modify(_.matchConditions).using(_ ::: newMatchConditions.toList)

  def check(newChecks: WsTextCheck*): WsTextFrameCheck =
    this.modify(_.checks).using(_ ::: newChecks.toList)
}
