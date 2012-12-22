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
import com.excilys.ebi.gatling.core.session.Expression

import scalaz._
import Scalaz._

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
			case Some(extracted) => value.success
			case _ => ("Check 'exists' failed, found " + value).failure
		}
	}

	val notExistsStrategy = new MatchStrategy[Any] {
		def apply(value: Option[Any], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || extracted.asInstanceOf[Seq[_]].isEmpty) => ("Check 'notExists' failed, found " + extracted).failure
			case _ => value.success
		}
	}

	val whateverStrategy = new MatchStrategy[Any] {
		def apply(value: Option[Any], session: Session) = value.success
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
			def apply(expression: XC) = extractorFactory(response)(expression).map(transformation)
		}
	})

	/**
	 * @param strategy the strategy for matching the extraction/transformation result
	 * @return a partial CheckBuilder
	 */
	def matchWith(strategy: MatchStrategy[X]) = {

		val matcher = new Matcher[R, XC] {

			def apply(response: R, session: Session, expression: XC): Validation[String, Any] = {

				val extractor = extractorFactory(response)
				val extractedValue = extractor(expression)
				strategy(extractedValue, session)
			}
		}

		new CheckBuilder(checkBuilderFactory, matcher) with SaveAsCheckBuilder[C, R, XC]
	}

	/**
	 * @param expected the expected value
	 * @return a partial CheckBuilder with a "is equal to" MatchStrategy
	 */
	def is(expected: Expression[X]) = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) => expected(session).flatMap { expectedValue =>
				if (expectedValue == extracted) value.success
				else ("Check 'is' failed, found " + extracted + " but expected " + expectedValue).failure
			}
			case None => "Check 'is' failed, found nothing".failure
		}
	})

	/**
	 * @param expected the expected value
	 * @return a partial CheckBuilder with a "is different from" MatchStrategy
	 */
	def not(expected: Expression[X]) = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) => expected(session).flatMap { expectedValue =>
				if (expectedValue != extracted) value.success
				else ("Check 'not' failed, found " + extracted + " but expected different from " + expectedValue).failure
			}
			case None => value.success
		}
	})

	def lessThan(expected: Expression[X]) = matchWith(new MatchStrategy[X] {

		def compare(expected: Any, extracted: X, ok: Boolean) =
			if (ok) Some(extracted).success
			else ("Check 'lessThan' failed, found " + extracted + " but expected " + expected).failure

		def compareByType(extracted: X)(expectedValue: Any) = {
			if (extracted.isInstanceOf[Long] & expectedValue.isInstanceOf[Long]) {
				compare(expectedValue, extracted, extracted.asInstanceOf[Long] <= expectedValue.asInstanceOf[Long])

			} else if (extracted.isInstanceOf[Int] & expectedValue.isInstanceOf[Int]) {
				compare(expectedValue, extracted, extracted.asInstanceOf[Int] <= expectedValue.asInstanceOf[Int])

			} else if (extracted.isInstanceOf[Double] & expectedValue.isInstanceOf[Double]) {
				compare(expectedValue, extracted, extracted.asInstanceOf[Double] <= expectedValue.asInstanceOf[Double])

			} else if (extracted.isInstanceOf[Float] & expectedValue.isInstanceOf[Float]) {
				compare(expectedValue, extracted, extracted.asInstanceOf[Float] <= expectedValue.asInstanceOf[Float])

			} else
				("Check 'lessThan' failed trying to compare thing that are not numbers of the same type, found " + extracted + " but expected " + expectedValue).failure
		}

		def apply(value: Option[X], session: Session) = value.map { extracted =>
			val expectedValue = expected(session)
			expectedValue.flatMap(compareByType(extracted))

		}.getOrElse("Check 'lessThan' failed, found nothing".failure)
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
	def in(expected: Expression[Seq[X]]) = matchWith(new MatchStrategy[X] {
		def apply(value: Option[X], session: Session) = value.map { extracted =>
			expected(session).flatMap { expectedValue=>
				if (expectedValue.contains(extracted)) value.success
				else ("Check 'in' failed, found " + extracted + " but expected " + expectedValue).failure
			}

		}.getOrElse(Failure("Check 'in' failed, found nothing"))
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