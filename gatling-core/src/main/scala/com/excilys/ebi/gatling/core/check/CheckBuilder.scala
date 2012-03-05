/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.EvaluatableString

trait ExtractorCheckBuilder[C <: Check[R], R, X] {

	def find: MatcherCheckBuilder[C, R, X]
}

trait MultipleExtractorCheckBuilder[C <: Check[R], R, X] extends ExtractorCheckBuilder[C, R, X] {

	def find(occurrence: Int): MatcherCheckBuilder[C, R, X]

	def findAll: MatcherCheckBuilder[C, R, Seq[X]]

	def count: MatcherCheckBuilder[C, R, Int]
}

class MatcherCheckBuilder[C <: Check[R], R, X](checkBuilderFactory: CheckBuilderFactory[C, R], extractorFactory: ExtractorFactory[R, X]) {

	def transform[T](transformation: X => T): MatcherCheckBuilder[C, R, T] = new MatcherCheckBuilder(checkBuilderFactory, new ExtractorFactory[R, T] {
		def apply(response: R) = new Extractor[T] {
			def apply(expression: String) = extractorFactory(response)(expression) match {
				case Some(x) => Some(transformation(x))
				case None => None
			}
		}
	})

	def matchWith(strategy: MatchStrategy[X]) = {

		val matcher: Matcher[R] = (expression: EvaluatableString, session: Session, response: R) => {
			val evaluatedExpression = expression(session)
			val extractor = extractorFactory(response)
			val extractedValue = extractor(evaluatedExpression)
			strategy(extractedValue, session)
		}

		new CheckBuilder(checkBuilderFactory, matcher) with SaveAsCheckBuilder[C, R]
	}

	def is(expected: Session => X) = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) => {
				val expectedValue = expected(session)
				if (extracted == expectedValue)
					Success(value)
				else
					Failure(new StringBuilder().append("Check 'is' failed, found ").append(extracted).append(" but expected ").append(expectedValue).toString)
			}
			case None => Failure("Check 'is' failed, found nothing")
		}
	})

	def not(expected: Session => X) = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case None => Success(value)
			case Some(extracted) => {
				val expectedValue = expected(session)
				if (extracted != expectedValue)
					Success(value)
				else
					Failure(new StringBuilder().append("Check 'not' failed, found ").append(extracted).append(" but expected different from ").append(expectedValue).toString)
			}
		}
	})

	def exists = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || !extracted.asInstanceOf[Seq[_]].isEmpty) => Success(value)
			case _ => Failure("Check 'exists' failed, found " + value)
		}
	})

	def notExists = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || extracted.asInstanceOf[Seq[_]].isEmpty) => Failure("Check 'notExists' failed, found " + extracted)
			case _ => Success(value)
		}
	})

	def in(expected: Session => Seq[X]) = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) => {
				val expectedValue = expected(session)
				if (expectedValue.contains(extracted))
					Success(value)
				else
					Failure(new StringBuilder().append("Check 'in' failed, found ").append(extracted).append(" but expected ").append(expectedValue).toString)
			}
			case None => Failure("Check 'in' failed, found nothing")
		}
	})
}

trait SaveAsCheckBuilder[C <: Check[R], R] extends CheckBuilder[C, R] {

	def saveAs(saveAs: String): CheckBuilder[C, R] = new CheckBuilder(checkBuilderFactory, matcher, Some(saveAs))
}

class CheckBuilder[C <: Check[R], R](val checkBuilderFactory: CheckBuilderFactory[C, R], val matcher: Matcher[R], saveAs: Option[String] = None) {

	def build: C = checkBuilderFactory(matcher, saveAs)
}