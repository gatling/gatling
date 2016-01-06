/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.annotation.tailrec
import scala.collection.mutable

import io.gatling.commons.validation._
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.session.{ Expression, Session }

object Check {

  def check[R](response: R, session: Session, checks: List[Check[R]]): (Session => Session, Option[Failure]) = {

    implicit val cache = mutable.Map.empty[Any, Any]

      @tailrec
      def checkRec(session: Session, checks: List[Check[R]], update: Session => Session, failure: Option[Failure]): (Session => Session, Option[Failure]) =
        checks match {

          case Nil => (update, failure)

          case head :: tail => head.check(response, session) match {
            case Success(checkResult) =>
              checkResult.update match {
                case Some(checkUpdate) =>
                  checkRec(
                    session = checkUpdate(session),
                    tail,
                    update = update andThen checkUpdate,
                    failure
                  )
                case _ =>
                  checkRec(session, tail, update, failure)
              }

            case f: Failure =>
              failure match {
                case None =>
                  checkRec(session, tail, update, Some(f))
                case _ => checkRec(session, tail, update, failure)
              }
          }
        }

    checkRec(session, checks, Session.Identity, None)
  }
}

trait Check[R] {

  def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult]
}

case class CheckBase[R, P, X](
    preparer:            Preparer[R, P],
    extractorExpression: Expression[Extractor[P, X]],
    validatorExpression: Expression[Validator[X]],
    saveAs:              Option[String]
) extends Check[R] {

  def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = {

      def memoizedPrepared: Validation[P] = cache
        .getOrElseUpdate(preparer, preparer(response))
        .asInstanceOf[Validation[P]]

    for {
      extractor <- extractorExpression(session).mapError(message => s"Check extractor resolution crashed: $message")
      validator <- validatorExpression(session).mapError(message => s"Check validator resolution crashed: $message")
      prepared <- memoizedPrepared.mapError(message => s"${extractor.name}.${extractor.arity}.${validator.name} failed, could not prepare: $message")
      actual <- extractor(prepared).mapError(message => s"${extractor.name}.${extractor.arity}.${validator.name} failed, could not extract: $message")
      matched <- validator(actual).mapError(message => s"${extractor.name}.${extractor.arity}.${validator.name}, $message")
    } yield CheckResult(matched, saveAs)
  }
}

object CheckResult {

  val NoopCheckResultSuccess = CheckResult(None, None).success
}

case class CheckResult(extractedValue: Option[Any], saveAs: Option[String]) {

  def hasUpdate = saveAs.isDefined && extractedValue.isDefined

  def update: Option[(Session => Session)] =
    for {
      s <- saveAs
      v <- extractedValue
    } yield (session: Session) => session.set(s, v)
}
