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

trait CheckBaseBuilder[C <: Check[R], R, X] {

	def find: VerifyBuilder[C, R, X]
}

trait MultipleOccurrence[C <: Check[R], R, X] extends CheckBaseBuilder[C, R, X] {

	def find(occurrence: Int): VerifyBuilder[C, R, X]

	def findAll: VerifyBuilder[C, R, Seq[X]]

	def count: VerifyBuilder[C, R, Int]
}

class VerifyBuilder[C <: Check[R], R, X](checkBuilderFactory: CheckBuilderFactory[C, R], extractorFactory: ExtractorFactory[R, X]) {

	def transform[T](transformation: X => T): VerifyBuilder[C, R, T] = new VerifyBuilder(checkBuilderFactory, new ExtractorFactory[R, T] {
		def apply(response: R) = new Extractor[T] {
			def apply(expression: String) = extractorFactory(response)(expression) match {
				case Some(x) => Some(transformation(x))
				case None => None
			}
		}
	})

	def verify(strategy: VerificationStrategy[X]) = {

		val verification: Verification[R] = (expression: EvaluatableString, session: Session, response: R) => {
			val evaluatedExpression = expression(session)
			val extractor = extractorFactory(response)
			val extractedValue = extractor(evaluatedExpression)
			strategy(extractedValue, session)
		}

		new CheckBuilder(checkBuilderFactory, verification) with SaveAsBuilder[C, R]
	}

	def is(expected: Session => X) = verify(new VerificationStrategy[X] {
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

	def not(expected: Session => X) = verify(new VerificationStrategy[X] {
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

	def exists = verify(new VerificationStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || !extracted.asInstanceOf[Seq[_]].isEmpty) => Success(value)
			case _ => Failure("Check 'exists' failed, found " + value)
		}
	})

	def notExists = verify(new VerificationStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) if (!extracted.isInstanceOf[Seq[_]] || extracted.asInstanceOf[Seq[_]].isEmpty) => Failure("Check 'notExists' failed, found " + extracted)
			case _ => Success(value)
		}
	})

	def in(expected: Session => Seq[X]) = verify(new VerificationStrategy[X] {
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

trait SaveAsBuilder[C <: Check[R], R] extends CheckBuilder[C, R] {

	def saveAs(saveAs: String): CheckBuilder[C, R] = new CheckBuilder(checkBuilderFactory, verification, Some(saveAs))
}

class CheckBuilder[C <: Check[R], R](val checkBuilderFactory: CheckBuilderFactory[C, R], val verification: Verification[R], saveAs: Option[String] = None) {

	def build: C = checkBuilderFactory(verification, saveAs)
}