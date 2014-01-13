/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.check.extractor

import java.util.regex.Matcher

import scala.annotation.tailrec

import io.gatling.core.check.extractor.regex.GroupExtractor

package object regex {

	implicit class RichMatcher(val matcher: Matcher) extends AnyVal {

		def foldLeft[T](zero: T)(f: (Matcher, T) => T): T = {
			var temp = zero
			while (matcher.find)
				temp = f(matcher, temp)
			temp
		}

		def findMatchN[X: GroupExtractor](n: Int): Option[X] = {

			@tailrec
			def findRec(countDown: Int): Boolean = matcher.find && (countDown == 0 || findRec(countDown - 1))

			if (findRec(n))
				Some(value[X])
			else
				None
		}

		def value[X: GroupExtractor]: X = implicitly[GroupExtractor[X]].extract(matcher)
	}
}
