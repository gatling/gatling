/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.util

object NumberHelper {

	def formatNumberWithSuffix(n: Long) = {
		val suffix = n % 10 match {
			case _ if (11 to 13) contains n % 100 => "th"
			case 1 => "st"
			case 2 => "nd"
			case 3 => "rd"
			case _ => "th"
		}

		n + suffix
	}

	def extractLongValue(s: String, start: Int): Long = {
		assume(start >= 0 && start < s.length, s"Start=$start is not an acceptable starting index for the string=$s")

		var value = 0l
		var k = start
		var c = ' ';
		while (k < s.length && { c = s.charAt(k); c.isDigit }) {
			value = value * 10l + c.getNumericValue
			k += 1
		}
		value
	}

	object IntString {
		def unapply(s: String): Option[Int] =
			if (s.forall(char => char >= '0' && char <= '9'))
				Some(s.toInt)
			else
				None
	}
}
