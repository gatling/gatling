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
package com.excilys.ebi.gatling.core

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz.ToValidationV

package object util {

	implicit class PaddableStringBuilder(sb: StringBuilder) {

		def appendTimes(s: Any, times: Int): PaddableStringBuilder = {
			for (i <- 1 to times) sb.append(s)
			this
		}

		def appendRightPaddedString(s: String, size: Int): PaddableStringBuilder = {
			sb.append(s)
			val paddingSize = size - s.length
			if (paddingSize > 0) {
				for (i <- 1 to paddingSize) sb.append(" ")
			}
			this
		}

		def appendLeftPaddedString(s: String, size: Int): PaddableStringBuilder = {
			val paddingSize = size - s.length
			if (paddingSize > 0) {
				for (i <- 1 to paddingSize) sb.append(" ")
			}
			sb.append(s)
			this
		}

		def append(s: String): PaddableStringBuilder = {
			sb.append(s)
			this
		}

		override def toString: String = sb.toString
	}

	implicit class FlattenableValidations[T](validations: List[Validation[String, T]]) {

		def flattenIt: Validation[String, List[T]] = {
			def sequenceRec(validations: List[Validation[String, T]]): Validation[String, List[T]] = validations match {
				case Nil => List.empty[T].success[String]
				case head :: tail => head match {
					case Success(entry) => sequenceRec(tail).map(entry :: _)
					case Failure(message) => message.failure[List[T]]
				}
			}
			sequenceRec(validations)
		}
	}
}