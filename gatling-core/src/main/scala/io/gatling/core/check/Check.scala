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

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation, ValidationList }

object Checks {

	def check[R](response: R, session: Session, checks: List[Check[R]]): Validation[Session => Session] = {

		implicit val cache = mutable.Map.empty[Any, Any]

		checks.map(_.check(response, session)).sequence.map(_.reduce(_ andThen _))
	}
}

trait Check[R] {

	def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[Session => Session]
}

case class CheckBase[R, P, T, X, E](
	preparer: Preparer[R, P],
	extractor: Extractor[P, T, X],
	extractorCriterion: Expression[T],
	matcher: Matcher[X, E],
	expectedExpression: Expression[E],
	saveAs: Option[String]) extends Check[R] {

	def check(response: R, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[Session => Session] = {

		def memoizedPrepared: Validation[P] = cache
			.getOrElseUpdate(preparer, preparer(response))
			.asInstanceOf[Validation[P]]

		def update(extractedValue: Option[Any]) = (session: Session) =>
			(for {
				key <- saveAs
				value <- extractedValue
			} yield session.set(key, value)).getOrElse(session)

		for {
			prepared <- memoizedPrepared.mapError(message => s"${extractor.name}.${matcher.name} failed, could not prepare: $message")
			criterion <- extractorCriterion(session).mapError(message => s"${extractor.name}.${matcher.name} failed: could not resolve extractor criterion: $message")
			actual <- extractor(prepared, criterion).mapError(message => s"${extractor.name}($criterion) failed: could not extract value: $message")
			expected <- expectedExpression(session).mapError(message => s"${extractor.name}($criterion).${matcher.name} failed: could not resolve expected value: $message")
			matched <- matcher(actual, expected).mapError(message => s"${extractor.name}($criterion).${matcher.name}($expected) didn't match: $message")

		} yield update(matched)
	}
}
