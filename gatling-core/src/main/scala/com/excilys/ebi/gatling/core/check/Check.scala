/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.check

import scala.collection.mutable

import com.excilys.ebi.gatling.core.session.{ Expression, Session }

import scalaz.Scalaz.{ ToBifunctorOps, ToValidationV }
import scalaz.Validation

object Checks {

	def check[R](response: R, session: Session, checks: Seq[Check[R]]): Validation[String, Session] = {

		implicit val preparedCache = mutable.Map.empty[Any, Any]

		checks.foldLeft(session.success[String]) { (newSession, check) =>
			newSession.flatMap(s => check.check(response, s))
		}
	}
}

trait Check[R] {

	type Cache = mutable.Map[Any, Any]

	def check(response: R, session: Session)(implicit cache: Cache): Validation[String, Session]
}

case class CheckBase[R, P, T, X, E](
	preparer: Preparer[R, P],
	extractor: Extractor[P, T, X],
	extractorCriterion: Expression[T],
	matcher: Matcher[X, E],
	expectedExpression: Expression[E],
	saveAs: Option[String]) extends Check[R] {

	def check(response: R, session: Session)(implicit cache: Cache): Validation[String, Session] = {

		// TODO find a way to avoid cast?
		// TODO use state monad?
		def memoizedPrepared: Validation[String, P] = cache
			.getOrElseUpdate(preparer, preparer(response))
			.asInstanceOf[Validation[String, P]]

		def doMatch(actual: Option[X], expected: E, criterion: T) =
			if (matcher(actual, expected))
				actual.success
			else
				s"${extractor.name}(${criterion}).${matcher.name}(${expected}) didn't match: $actual".failure

		def newSession(extractedValue: Option[Any]) =
			(for {
				key <- saveAs
				value <- extractedValue
			} yield session.set(key, value)).getOrElse(session)

		for {
			prepared <- memoizedPrepared.<-:(message => s"${extractor.name}.${matcher.name} failed, could not prepare: $message")
			criterion <- extractorCriterion(session).<-:(message => s"${extractor.name}.${matcher.name} failed: could not resolve extractor criterion: $message")
			actual <- extractor(prepared, criterion).<-:(message => s"${extractor.name}(${criterion}) failed: could not extract value: $message")
			expected <- expectedExpression(session).<-:(message => s"${extractor.name}(${criterion}).${matcher.name} failed: could not resolve expected value: $message")
			matched <- doMatch(actual, expected, criterion)

		} yield newSession(matched)
	}
}