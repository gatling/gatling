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

trait CheckBaseBuilder[C <: Check[R, X], R, X] {
	def find: CheckOneBuilder[C, R, X]
}

trait MultipleOccurrence[C <: Check[R, X], CM <: Check[R, Seq[X]], CC <: Check[R, Int], R, X] extends CheckBaseBuilder[C, R, X] {

	def find(occurrence: Int): CheckOneBuilder[C, R, X]

	def findAll: CheckMultipleBuilder[CM, R, Seq[X]]

	def count: CheckOneBuilder[CC, R, Int]
}

class CheckOneBuilder[C <: Check[R, X], R, X](checkBuilderFactory: CheckBuilderFactory[C, R, X], extractorFactory: ExtractorFactory[R, X]) {

	def verify[XP](strategy: CheckStrategy[X]) = new CheckBuilder(checkBuilderFactory, extractorFactory, strategy) with SaveAsBuilder[C, R, X]

	def exists = verify(new CheckStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(_) => Success(value)
			case None => Failure("Check 'exists' failed")
		}
	})

	def notExists = verify(new CheckStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case None => Success(value)
			case Some(extracted) => Failure("Check 'notExists' failed, found " + extracted)
		}
	})

	def is(expected: Session => X) = verify(new CheckStrategy[X] {
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

	def not(expected: Session => X) = verify(new CheckStrategy[X] {
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

	def in(expected: Session => Seq[X]) = verify(new CheckStrategy[X] {
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

class CheckMultipleBuilder[C <: Check[R, X], R, X <: Seq[_]](checkBuilderFactory: CheckBuilderFactory[C, R, X], extractorFactory: ExtractorFactory[R, X]) {

	def verify[XP](strategy: CheckStrategy[X]) = new CheckBuilder(checkBuilderFactory, extractorFactory, strategy) with SaveAsBuilder[C, R, X]

	def notEmpty = verify(new CheckStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) =>
				if (!extracted.isEmpty)
					Success(value)
				else
					Failure("Check 'notEmpty' failed, found empty")
			case None => Failure("Check 'notEmpty' failed, found None")
		}
	})

	def empty = verify(new CheckStrategy[X] {
		def apply(value: Option[X], session: Session) = value match {
			case Some(extracted) =>
				if (extracted.isEmpty)
					Success(value)
				else
					Failure("Check 'empty' failed, found " + extracted)
			case None => Failure("Check 'empty' failed, found None")
		}
	})

	def is(expected: Session => X) = verify(new CheckStrategy[X] {
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
}

trait SaveAsBuilder[C <: Check[R, X], R, X] extends CheckBuilder[C, R, X] {

	def saveAs(saveAs: String): CheckBuilder[C, R, X] = new CheckBuilder(checkBuilderFactory, extractorFactory, strategy, Some(saveAs))

	def saveAs(saveAs: String, transform: X => Any): CheckBuilder[C, R, X] = new CheckBuilder(checkBuilderFactory, extractorFactory, strategy, Some(saveAs), Some(transform))
}

class CheckBuilder[C <: Check[R, X], R, X](val checkBuilderFactory: CheckBuilderFactory[C, R, X], val extractorFactory: ExtractorFactory[R, X], val strategy: CheckStrategy[X], saveAs: Option[String] = None, transform: Option[X => Any] = None) {

	def build: C = checkBuilderFactory(extractorFactory, strategy, saveAs, transform)
}