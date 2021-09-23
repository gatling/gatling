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

package io.gatling.http.check.ws

import scala.concurrent.duration.FiniteDuration

import com.softwaremill.quicklens._

final case class WsFrameCheckSequence[+T <: WsFrameCheck](timeout: FiniteDuration, checks: List[T]) {
  require(checks.nonEmpty, "Can't pass empty check sequence")
}

sealed trait WsFrameCheck {
  def name: String
  def isSilent: Boolean
}

object WsFrameCheck {
  final case class Binary(name: String, matchConditions: List[WsCheck.Binary], checks: List[WsCheck.Binary], isSilent: Boolean) extends WsFrameCheck {

    def matching(newMatchConditions: WsCheck.Binary*): Binary = {
      require(!newMatchConditions.contains(null), "Matching conditions can't contain null elements. Forward reference issue?")
      this.modify(_.matchConditions)(_ ::: newMatchConditions.toList)
    }

    def check(newChecks: WsCheck.Binary*): Binary = {
      require(!newChecks.contains(null), "Checks can't contain null elements. Forward reference issue?")
      this.modify(_.checks)(_ ::: newChecks.toList)
    }

    def silent: Binary =
      copy(isSilent = true)
  }

  final case class Text(name: String, matchConditions: List[WsCheck.Text], checks: List[WsCheck.Text], isSilent: Boolean) extends WsFrameCheck {

    def matching(newMatchConditions: WsCheck.Text*): Text = {
      require(!newMatchConditions.contains(null), "Matching conditions can't contain null elements. Forward reference issue?")
      this.modify(_.matchConditions)(_ ::: newMatchConditions.toList)
    }

    def check(newChecks: WsCheck.Text*): Text = {
      require(!newChecks.contains(null), "Checks can't contain null elements. Forward reference issue?")
      this.modify(_.checks)(_ ::: newChecks.toList)
    }

    def silent: Text =
      copy(isSilent = true)
  }
}
