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

/**
 * A partial CheckBuilder
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 * @param <X> the type of extracted value
 */
abstract class ExtractorCheckBuilder[C <: Check[R, XC], R, XC, X] {

	/**
	 * @return a partial CheckBuilder with an Extractor for the first value
	 */
	def find: MatcherCheckBuilder[C, R, XC, X]
}

/**
 * A partial CheckBuilder that might produce multiple values
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 * @param <X> the type of extracted value
 */
trait MultipleExtractorCheckBuilder[C <: Check[R, XC], R, XC, X] extends ExtractorCheckBuilder[C, R, XC, X] {

	/**
	 * @return a partial CheckBuilder with an Extractor for the given occurrence
	 */
	def find(occurrence: Int): MatcherCheckBuilder[C, R, XC, X]

	/**
	 * @return a partial CheckBuilder with an Extractor for all the occurrences
	 */
	def findAll: MatcherCheckBuilder[C, R, XC, Seq[X]]

	/**
	 * @return a partial CheckBuilder with an Extractor for the count of occurrences
	 */
	def count: MatcherCheckBuilder[C, R, XC, Int]
}

object MatcherCheckBuilder {
	val existsStrategy = new MatchStrategy[Any] {
		def apply(value: Option[Any], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || !extracted.asInstanceOf[Seq[_]].isEmpty) => Success(value)
			case _ => Failure("Check 'exists' failed, found " + value)
		}
	}

	val notExistsStrategy = new MatchStrategy[Any] {
		def apply(value: Option[Any], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || extracted.asInstanceOf[Seq[_]].isEmpty) => Failure("Check 'notExists' failed, found " + extracted)
			case _ => Success(value)
		}
	}

	val whateverStrategy = new MatchStrategy[Any] {
		def apply(value: Option[Any], session: Session) = Success(value)
	}
}

/**
 * A partial CheckBuilder that might transform and match the extracted value
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 * @param <X> the type of extracted value
 */
class MatcherCheckBuilder[C <: Check[R, XC], R, XC, X](checkBuilderFactory: CheckBuilderFactory[C, R, XC], extractorFactory: ExtractorFactory[R, XC, X]) {

	/**
	 * @param transformation a function for transforming the extracted value of type X into a value of type T
	 * @return a partial CheckBuilder
	 */
	def transform[T](transformation: X => T): MatcherCheckBuilder[C, R, XC, T] = new MatcherCheckBuilder(checkBuilderFactory, new ExtractorFactory[R, XC, T] {
		def apply(response: R) = new Extractor[XC, T] {
			def apply(expression: XC) = extractorFactory(response)(expression) match {
				case Some(x) => Some(transformation(x))
				case None => None
			}
		}
	})

	/**
	 * @param strategy the strategy for matching the extraction/transformation result
	 * @return a partial CheckBuilder
	 */
	def matchWith(strategy: MatchStrategy[X]) = {

		val matcher = new Matcher[R, XC] {
			def apply(expression: Session => XC, session: Session, response: R) = {
				val evaluatedExpression = expression(session)
				val extractor = extractorFactory(response)
				val extractedValue = extractor(evaluatedExpression)
				strategy(extractedValue, session)
			}
		}

		new CheckBuilder(checkBuilderFactory, matcher) with SaveAsCheckBuilder[C, R, XC]
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

	def lessThan(expected: Session => X) = matchWith(new MatchStrategy[X] {

		def compare(expected: X, extracted: X, ok: Boolean) = {
			if (ok)
				Success(Some(extracted))
			else
				Failure(new StringBuilder().append("Check 'lessThan' failed, found ").append(extracted).append(" but expected ").append(expected).toString)
		}

		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) => {
				val expectedValue = expected(session)

				if (extracted.isInstanceOf[Long] & expectedValue.isInstanceOf[Long]) {
					compare(expectedValue, extracted, extracted.asInstanceOf[Long] <= expectedValue.asInstanceOf[Long])

				} else if (extracted.isInstanceOf[Int] & expectedValue.isInstanceOf[Int]) {
					compare(expectedValue, extracted, extracted.asInstanceOf[Int] <= expectedValue.asInstanceOf[Int])

				} else if (extracted.isInstanceOf[Double] & expectedValue.isInstanceOf[Double]) {
					compare(expectedValue, extracted, extracted.asInstanceOf[Double] <= expectedValue.asInstanceOf[Double])

				} else if (extracted.isInstanceOf[Float] & expectedValue.isInstanceOf[Float]) {
					compare(expectedValue, extracted, extracted.asInstanceOf[Float] <= expectedValue.asInstanceOf[Float])

				} else
					Failure(new StringBuilder().append("Check 'lessThan' failed trying to compare thing that are not numbers of the same type, found ").append(extracted).append(" but expected ").append(expectedValue).toString)
			}
			case None => Failure("Check 'lessThan' failed, found nothing")
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
	def exists = matchWith(MatcherCheckBuilder.existsStrategy)

	/**
	 * @return a partial CheckBuilder with a "is not defined or is an empty Seq" MatchStrategy
	 */
	def notExists = matchWith(MatcherCheckBuilder.notExistsStrategy)

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

	/**
	 * @return a partial CheckBuilder with a "maybe (always true)" MatchStrategy
	 */
	def whatever = matchWith(MatcherCheckBuilder.whateverStrategy)
}

/**
 * A partial CheckBuilder that might save the extracted/transformed value into the session if the checks succeed
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 */
trait SaveAsCheckBuilder[C <: Check[R, XC], R, XC] extends CheckBuilder[C, R, XC] {

	def saveAs(saveAs: String): CheckBuilder[C, R, XC] = new CheckBuilder(checkBuilderFactory, matcher, Some(saveAs))
}

/**
 * A complete CheckBuilder
 *
 * @param <C> the type of Check
 * @param <R> the type of response
 */
class CheckBuilder[C <: Check[R, XC], R, XC](val checkBuilderFactory: CheckBuilderFactory[C, R, XC], val matcher: Matcher[R, XC], saveAs: Option[String] = None) {

	def build: C = checkBuilderFactory(matcher, saveAs)
}