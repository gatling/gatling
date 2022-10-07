/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation.Validation
import io.gatling.core.session.{ Expression, Session }

import com.softwaremill.quicklens._

final case class WsFrameCheckSequence[+T <: WsFrameCheck](timeout: FiniteDuration, checks: List[T]) {
  require(checks.nonEmpty, "Can't pass an empty check sequence")
}

sealed trait WsFrameCheck extends Product with Serializable {
  def resolvedName: String
  def isSilent: Boolean
}

object WsFrameCheck {
  final case class Binary(
      name: Expression[String],
      matchConditions: List[WsCheck.Binary],
      checks: List[WsCheck.Binary],
      isSilent: Boolean,
      resolvedName: String
  ) extends WsFrameCheck {
    def matching(newMatchConditions: WsCheck.Binary*): Binary = {
      require(!newMatchConditions.contains(null), "Matching conditions can't contain null elements. Forward reference issue?")
      this.modify(_.matchConditions)(_ ::: newMatchConditions.toList)
    }

    def check(newChecks: WsCheck.Binary*): Binary = {
      require(!newChecks.contains(null), "Checks can't contain null elements. Forward reference issue?")
      this.modify(_.checks)(_ ::: newChecks.toList)
    }

    def checkIf(condition: Expression[Boolean])(thenChecks: WsCheck.Binary*): Binary =
      check(thenChecks.map(_.checkIf(condition)): _*)

    def checkIf(condition: (Array[Byte], Session) => Validation[Boolean])(thenChecks: WsCheck.Binary*): Binary =
      check(thenChecks.map(_.checkIf(condition)): _*)

    def silent: Binary =
      copy(isSilent = true)
  }

  final case class Text(
      name: Expression[String],
      matchConditions: List[WsCheck.Text],
      checks: List[WsCheck.Text],
      isSilent: Boolean,
      resolvedName: String
  ) extends WsFrameCheck {
    def matching(newMatchConditions: WsCheck.Text*): Text = {
      require(!newMatchConditions.contains(null), "Matching conditions can't contain null elements. Forward reference issue?")
      this.modify(_.matchConditions)(_ ::: newMatchConditions.toList)
    }

    def check(newChecks: WsCheck.Text*): Text = {
      require(!newChecks.contains(null), "Checks can't contain null elements. Forward reference issue?")
      this.modify(_.checks)(_ ::: newChecks.toList)
    }

    def checkIf(condition: Expression[Boolean])(thenChecks: WsCheck.Text*): Text =
      check(thenChecks.map(_.checkIf(condition)): _*)

    def checkIf(condition: (String, Session) => Validation[Boolean])(thenChecks: WsCheck.Text*): Text =
      check(thenChecks.map(_.checkIf(condition)): _*)

    def silent: Text =
      copy(isSilent = true)
  }
}
