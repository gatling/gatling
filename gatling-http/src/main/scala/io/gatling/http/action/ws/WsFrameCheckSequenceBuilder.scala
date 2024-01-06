/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.check.ws.{ WsFrameCheck, WsFrameCheckSequence }

object WsFrameCheckSequenceBuilder {
  def resolve[T <: WsFrameCheck](builders: List[WsFrameCheckSequenceBuilder[T]], session: Session): Validation[List[WsFrameCheckSequence[T]]] = {
    @tailrec
    def resolveRec(builders: List[WsFrameCheckSequenceBuilder[T]], acc: List[WsFrameCheckSequence[T]]): Validation[List[WsFrameCheckSequence[T]]] =
      builders match {
        case Nil => acc.reverse.success
        case builder :: tail =>
          val sequenceV =
            for {
              timeout <- builder.timeout(session)
              checks <- resolveChecks(builder.checks, session)
            } yield WsFrameCheckSequence(timeout, checks)

          sequenceV match {
            case Success(sequence) => resolveRec(tail, sequence :: acc)
            case failure: Failure  => failure
          }
      }

    resolveRec(builders, Nil)
  }

  private def resolveChecks[T <: WsFrameCheck](checks: List[T], session: Session): Validation[List[T]] = {
    @tailrec
    def resolveChecksRec(checks: List[T], acc: List[T]): Validation[List[T]] =
      checks match {
        case Nil => acc.reverse.success
        case check :: tail =>
          val resolvedCheckV = check match {
            case text: WsFrameCheck.Text     => text.name(session).map(resolvedName => text.copy(resolvedName = resolvedName))
            case binary: WsFrameCheck.Binary => binary.name(session).map(resolvedName => binary.copy(resolvedName = resolvedName))
          }

          resolvedCheckV match {
            case Success(resolvedCheck) => resolveChecksRec(tail, resolvedCheck.asInstanceOf[T] :: acc)
            case failure: Failure       => failure
          }
      }

    resolveChecksRec(checks, Nil)
  }
}

final case class WsFrameCheckSequenceBuilder[+T <: WsFrameCheck](timeout: Expression[FiniteDuration], checks: List[T])
