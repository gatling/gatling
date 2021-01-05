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

package io.gatling.core.check

import java.{ util => ju }

import scala.annotation.tailrec

import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session }

object Check {

  type PreparedCache = ju.Map[Any, Any]

  def newPreparedCache: PreparedCache =
    new ju.HashMap(2)

  def check[R](response: R, session: Session, checks: List[Check[R]]): (Session, Option[Failure]) = {

    val preparedCache: PreparedCache =
      if (checks.size > 1) {
        newPreparedCache
      } else {
        null
      }

    check(response, session, checks, preparedCache)
  }

  def check[R](response: R, session: Session, checks: List[Check[R]], preparedCache: PreparedCache): (Session, Option[Failure]) = {

    @tailrec
    def checkRec(currentSession: Session, checks: List[Check[R]], failure: Option[Failure]): (Session, Option[Failure]) =
      checks match {
        case Nil => (currentSession, failure)

        case check :: tail =>
          check.check(response, currentSession, preparedCache) match {
            case Success(checkResult) =>
              val newSession = checkResult.update(currentSession)
              checkRec(newSession, tail, failure)

            case f: Failure =>
              checkRec(currentSession, tail, if (failure.isDefined) failure else Some(f))
          }
      }

    checkRec(session, checks, None)
  }
}

trait Check[R] {

  def check(response: R, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult]
}

final class CheckBase[R, P, X](
    val preparer: Preparer[R, P],
    val extractorExpression: Expression[Extractor[P, X]],
    val validatorExpression: Expression[Validator[X]],
    val displayActualValue: Boolean,
    val customName: Option[String],
    val saveAs: Option[String]
) extends Check[R] {

  def check(response: R, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {

    def unbuiltName: String = customName.getOrElse("Check")
    def memoizedPrepared: Validation[P] =
      if (preparedCache == null) {
        preparer(response)
      } else {
        preparedCache.computeIfAbsent(preparer, _ => preparer(response)).asInstanceOf[Validation[P]]
      }
    def builtName(extractor: Extractor[P, X], validator: Validator[X]): String = customName.getOrElse(s"${extractor.name}.${extractor.arity}.${validator.name}")

    for {
      extractor <- extractorExpression(session).mapError(message => s"$unbuiltName extractor resolution crashed: $message")
      validator <- validatorExpression(session).mapError(message => s"$unbuiltName validator resolution crashed: $message")
      prepared <- memoizedPrepared.mapError(message => s"${builtName(extractor, validator)} preparation crashed: $message")
      actual <- extractor(prepared).mapError(message => s"${builtName(extractor, validator)} extraction crashed: $message")
      matched <- validator(actual, displayActualValue).mapError(message => s"${builtName(extractor, validator)}, $message")
    } yield new CheckResult(matched, saveAs)
  }
}

object CheckResult {

  val NoopCheckResultSuccess: Validation[CheckResult] = new CheckResult(None, None).success
}

final case class CheckResult(extractedValue: Option[Any], saveAs: Option[String]) {

  def update(session: Session): Session = {
    val maybeUpdatedSession =
      for {
        s <- saveAs
        v <- extractedValue
      } yield session.set(s, v)
    maybeUpdatedSession.getOrElse(session)
  }
}
