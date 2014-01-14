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
package io.gatling.core.filter

import scala.annotation.tailrec
import scala.util.matching.Regex

case class Filters(first: Filter, second: Filter) {

	def accept(url: String) = first.accept(url) && second.accept(url)
}

sealed abstract class Filter {
	def patterns: List[String]
	val regexs = patterns.map(_.r)
	def accept(url: String): Boolean
}

case class WhiteList(patterns: List[String] = Nil) extends Filter {

	def accept(url: String): Boolean = {

		@tailrec
		def acceptRec(regexs: List[Regex]): Boolean = regexs match {
			case Nil => false
			case head :: tail => head.pattern.matcher(url).matches || acceptRec(tail)
		}

		regexs.isEmpty || acceptRec(regexs)
	}
}

case class BlackList(patterns: List[String] = Nil) extends Filter {

	def accept(url: String): Boolean = {

		@tailrec
		def acceptRec(regexs: List[Regex]): Boolean = regexs match {
			case Nil => true
			case head :: tail => !head.pattern.matcher(url).matches && acceptRec(tail)
		}

		acceptRec(regexs)
	}
}
