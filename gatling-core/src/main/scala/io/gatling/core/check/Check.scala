/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.collection.mutable

import io.gatling.core.session.Session
import io.gatling.core.validation.{ SuccessWrapper, Validation, ValidationList }
import io.gatling.core.check.extractor.Extractor

object Checks {

	val noopUpdate = (identity[Session] _).success

	def check[R](response: R, session: Session, checks: List[Check[R]]): Validation[Session => Session] = {

		implicit val cache = mutable.Map.empty[Any, Any]

		checks match {
			case Nil => noopUpdate
			case checks => checks.map(_.check(response, session)).sequence.map(_.reduce(_ andThen _))
		}
	}
}

trait Check[R] {

	def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[Session => Session]
}

case class CheckBase[R, P, X](
	preparer: Preparer[R, P],
	extractor: Extractor[P, X],
	validator: Validator[X],
	saveAs: Option[String]) extends Check[R] {

	def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[Session => Session] = {

		def update(extractedValue: Option[Any]) = (session: Session) =>
			(for {
				key <- saveAs
				value <- extractedValue
			} yield session.set(key, value)).getOrElse(session)

		val memoizedPrepared: Validation[P] = cache
			.getOrElseUpdate(preparer, preparer(response))
			.asInstanceOf[Validation[P]]

		for {
			prepared <- memoizedPrepared.mapError(message => s"${extractor.name}.${validator.name} failed, could not prepare: $message")
			actual <- extractor(session, prepared).mapError(message => s"${extractor.name}.${validator.name} failed: could not extract: $message")
			matched <- validator(session, actual).mapError(message => s"${extractor.name}.${validator.name} didn't match: $message")

		} yield update(matched)
	}
}
