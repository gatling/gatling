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

/**
 * A partial CheckBuilder
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 * @param <X> the type of extracted value
 */
trait ExtractorCheckBuilder[C <: Check[R], R, X] {

	/**
	 * @return a partial CheckBuilder with an Extractor for the first value
	 */
	def find: MatcherCheckBuilder[C, R, X]
}

/**
 * A partial CheckBuilder that might produce multiple values
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 * @param <X> the type of extracted value
 */
trait MultipleExtractorCheckBuilder[C <: Check[R], R, X] extends ExtractorCheckBuilder[C, R, X] {

	/**
	 * @return a partial CheckBuilder with an Extractor for the given occurrence
	 */
	def find(occurrence: Int): MatcherCheckBuilder[C, R, X]

	/**
	 * @return a partial CheckBuilder with an Extractor for all the occurrences
	 */
	def findAll: MatcherCheckBuilder[C, R, Seq[X]]

	/**
	 * @return a partial CheckBuilder with an Extractor for the count of occurrences
	 */
	def count: MatcherCheckBuilder[C, R, Int]
}

/**
 * A partial CheckBuilder that might transform and match the extracted value
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 * @param <X> the type of extracted value
 */
class MatcherCheckBuilder[C <: Check[R], R, X](checkBuilderFactory: CheckBuilderFactory[C, R], extractorFactory: ExtractorFactory[R, X]) {

	/**
	 * @param transformation a function for transforming the extracted value of type X into a value of type T
	 * @return a partial CheckBuilder
	 */
	def transform[T](transformation: X => T): MatcherCheckBuilder[C, R, T] = new MatcherCheckBuilder(checkBuilderFactory, new ExtractorFactory[R, T] {
		def apply(response: R) = new Extractor[T] {
			def apply(expression: String) = extractorFactory(response)(expression) match {
				case Some(x) => Some(transformation(x))
				case None => None
			}
		}
	})

	/**
	 * @param strategy the strategy for matching the extraction/trasformation result
	 * @return a partial CheckBuilder
	 */
	def matchWith(strategy: MatchStrategy[X]) = {

		val matcher: Matcher[R] = (expression: EvaluatableString, session: Session, response: R) => {
			val evaluatedExpression = expression(session)
			val extractor = extractorFactory(response)
			val extractedValue = extractor(evaluatedExpression)
			strategy(extractedValue, session)
		}

		new CheckBuilder(checkBuilderFactory, matcher) with SaveAsCheckBuilder[C, R]
	}

	/**
	 * @param expected the expected value
	 * @return a partial CheckBuilder with a "is equal to" MatchStrategy
	 */
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

	/**
	 * @param expected the expected value
	 * @return a partial CheckBuilder with a "is different from" MatchStrategy
	 */
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

	/**
	 * @return a partial CheckBuilder with a "is defined and is not an empty Seq" MatchStrategy
	 */
	def exists = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || !extracted.asInstanceOf[Seq[_]].isEmpty) => Success(value)
			case _ => Failure("Check 'exists' failed, found " + value)
		}
	})

	/**
	 * @return a partial CheckBuilder with a "is not defined or is an empty Seq" MatchStrategy
	 */
	def notExists = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || extracted.asInstanceOf[Seq[_]].isEmpty) => Failure("Check 'notExists' failed, found " + extracted)
			case _ => Success(value)
		}
	})

	/**
	 * @param expected the expected sequence
	 * @return a partial CheckBuilder with a "belongs to the sequence" MatchStrategy
	 */
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

/**
 * A partial CheckBuilder that might save the extracted/transformed value into the session if the checks succeed
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 */
trait SaveAsCheckBuilder[C <: Check[R], R] extends CheckBuilder[C, R] {

	def saveAs(saveAs: String): CheckBuilder[C, R] = new CheckBuilder(checkBuilderFactory, matcher, Some(saveAs))
}

/**
 * A complete CheckBuilder
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 */
class CheckBuilder[C <: Check[R], R](val checkBuilderFactory: CheckBuilderFactory[C, R], val matcher: Matcher[R], saveAs: Option[String] = None) {

	def build: C = checkBuilderFactory(matcher, saveAs)
}