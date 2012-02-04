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
import com.excilys.ebi.gatling.core.check.extractor.ExtractorFactory

trait CheckBaseBuilder[C <: Check[R, X], R, X] {
	def find: CheckOneBuilder[C, R, X]
}

trait MultipleOccurence[C <: Check[R, X], CM <: Check[R, List[X]], R, X] extends CheckBaseBuilder[C, R, X] {

	def find(occurrence: Int): CheckOneBuilder[C, R, X]

	def findAll: CheckMultipleBuilder[CM, R, List[X]]
}

class CheckOneBuilder[C <: Check[R, X], R, X](f: (ExtractorFactory[R, X], CheckStrategy[X], Option[String]) => C, extractorFactory: ExtractorFactory[R, X]) {

	def verify[XP](strategy: CheckStrategy[X]) = new CheckBuilder(f, extractorFactory, strategy) with SaveAsBuilder[C, R, X]

	def exists = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case Some(_) => CheckResult(true, value)
			case None => CheckResult(false, None, Some("Check 'exists' failed"))
		}
	})

	def notExists = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case None => CheckResult(true, value)
			case Some(extracted) => CheckResult(false, None, Some("Check 'notExists' failed, found " + extracted))
		}
	})

	def is(expected: Session => X) = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case Some(extracted) => {
				val expectedValue = expected(s)
				if (extracted == expectedValue)
					CheckResult(true, value)
				else
					CheckResult(false, value, Some("Check 'eq' failed, found " + extracted + " but expected " + expectedValue))
			}
			case None => CheckResult(false, None, Some("Check 'eq' failed, found nothing"))
		}
	})

	def not(expected: Session => X) = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case None => CheckResult(true, value)
			case Some(extracted) => {
				val expectedValue = expected(s)
				if (extracted != expectedValue)
					CheckResult(true, value)
				else
					CheckResult(false, None, Some("Check 'neq' failed, found " + extracted + " but expected !" + expectedValue))
			}
		}
	})

	def in(expected: Session => Seq[X]) = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case Some(extracted) => {
				val expectedValue = expected(s)
				if (expectedValue.contains(extracted))
					CheckResult(true, value)
				else
					CheckResult(false, None, Some("Check 'in' failed, found " + extracted + " but expected " + expectedValue))
			}
			case None => CheckResult(false, None, Some("Check 'eq' failed, found nothing"))
		}
	})
}

class CheckMultipleBuilder[C <: Check[R, X], R, X <: List[_]](f: (ExtractorFactory[R, X], CheckStrategy[X], Option[String]) => C, extractorFactory: ExtractorFactory[R, X]) {

	def verify[XP](strategy: CheckStrategy[X]) = new CheckBuilder(f, extractorFactory, strategy) with SaveAsBuilder[C, R, X]

	def notEmpty = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case Some(extracted) =>
				if (!extracted.isEmpty)
					CheckResult(true, value)
				else
					CheckResult(false, None, Some("Check 'notEmpty' failed, found empty"))
			case None => CheckResult(false, None, Some("Check 'notEmpty' failed, found None"))
		}
	})

	def empty = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case Some(extracted) =>
				if (extracted.isEmpty)
					CheckResult(true, value)
				else
					CheckResult(false, None, Some("Check 'empty' failed, found " + extracted))
			case None => CheckResult(false, None, Some("Check 'empty' failed, found None"))
		}
	})

	def is(expected: Session => X) = verify(new CheckStrategy[X] {
		def apply(value: Option[X], s: Session) = value match {
			case Some(extracted) => {
				val expectedValue = expected(s)
				if (extracted == expectedValue)
					CheckResult(true, value)
				else
					CheckResult(false, None, Some("Check 'eq' failed, found " + extracted + " but expected " + expectedValue))
			}
			case None => CheckResult(false, None, Some("Check 'eq' failed, found nothing"))
		}
	})
}

trait SaveAsBuilder[C <: Check[R, X], R, X] extends CheckBuilder[C, R, X] {

	def saveAs(saveAs: String) = new CheckBuilder(f, extractorFactory, strategy, Some(saveAs))
}

class CheckBuilder[C <: Check[R, X], R, X](val f: (ExtractorFactory[R, X], CheckStrategy[X], Option[String]) => C, val extractorFactory: ExtractorFactory[R, X], val strategy: CheckStrategy[X], saveAs: Option[String] = None) {

	def build: C = f(extractorFactory, strategy, saveAs)
}

