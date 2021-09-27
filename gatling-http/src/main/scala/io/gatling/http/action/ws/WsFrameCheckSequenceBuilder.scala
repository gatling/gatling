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

package io.gatling.http.action.ws

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.check.ws.{ WsFrameCheck, WsFrameCheckSequence }

object WsFrameCheckSequenceBuilder {

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  def resolve[T <: WsFrameCheck](builders: List[WsFrameCheckSequenceBuilder[T]], session: Session): Validation[List[WsFrameCheckSequence[T]]] = {
    builders.foldLeft(List.empty[WsFrameCheckSequence[T]].success) { (acc, builder) =>
      for {
        timeout <- builder.timeout(session)
        accValue <- acc
      } yield accValue :+ WsFrameCheckSequence(timeout, resolveCheckName(builder.checks, session).asInstanceOf[List[T]])
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  private def resolveCheckName[T <: WsFrameCheck](checks: List[WsFrameCheck], session: Session): List[WsFrameCheck] = {
    checks
      .map {
        case check: WsFrameCheck.Text =>
          check.checkName(session) match {
            case Success(value) => check.copy(name = value)
          }
        case check: WsFrameCheck.Binary =>
          check.checkName(session) match {
            case Success(value) => check.copy(name = value)
          }
      }
  }
}

final case class WsFrameCheckSequenceBuilder[+T <: WsFrameCheck](timeout: Expression[FiniteDuration], checks: List[T])
