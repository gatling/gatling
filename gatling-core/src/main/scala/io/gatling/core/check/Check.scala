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
package io.gatling.core.check

import scala.annotation.tailrec
import scala.collection.mutable

import io.gatling.core.check.extractor.Extractor
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Success, SuccessWrapper, Validation }

object Check {

  val noopUpdate = identity[Session] _
  val noopUpdateSuccess = noopUpdate.success

  def check[R](response: R, session: Session, checks: List[Check[R]]): Validation[Session => Session] = {

    implicit val cache = mutable.Map.empty[Any, Any]

      @tailrec
      def checkRec(checks: List[Check[R]], updates: Validation[Session => Session]): Validation[Session => Session] = checks match {
        case Nil => updates
        case head :: tail => head.check(response, session) match {
          case Success(update) =>
            val newUpdates = updates.map(_ andThen update)
            checkRec(tail, newUpdates)
          case failure => failure
        }
      }

    checkRec(checks, noopUpdateSuccess)
  }
}

trait Check[R] {

  def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[Session => Session]
}

case class CheckBase[R, P, X](
    preparer: Preparer[R, P],
    extractorExpression: Expression[Extractor[P, X]],
    validatorExpression: Expression[Validator[X]],
    saveAs: Option[String]) extends Check[R] {

  def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[Session => Session] = {

      def update(extractedValue: Option[Any]) =
        (for {
          key <- saveAs
          value <- extractedValue
        } yield (session: Session) => session.set(key, value)).getOrElse(Check.noopUpdate)

    val memoizedPrepared: Validation[P] = cache
      .getOrElseUpdate(preparer, preparer(response))
      .asInstanceOf[Validation[P]]

    for {
      extractor <- extractorExpression(session).mapError(message => s"Check extractor resolution crashed: $message")
      validator <- validatorExpression(session).mapError(message => s"Check validator resolution crashed: $message")
      prepared <- memoizedPrepared.mapError(message => s"${extractor.name}.${validator.name} failed, could not prepare: $message")
      actual <- extractor(prepared).mapError(message => s"${extractor.name}.${validator.name} failed, could not extract: $message")
      matched <- validator(actual).mapError(message => s"${extractor.name}.${validator.name}, $message")

    } yield update(matched)
  }
}
