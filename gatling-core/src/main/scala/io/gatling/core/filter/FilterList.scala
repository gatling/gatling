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
package io.gatling.core.filter

import scala.annotation.tailrec
import scala.util.matching.Regex

object FilterList {

	@tailrec
	def accept(string: String, filters: List[FilterList]): Boolean = filters match {
		case Nil => true
		case head :: tail =>
			if (head.accept(string))
				accept(string, tail)
			else
				false
	}
}

sealed trait FilterList { def accept(url: String): Boolean }

case class WhiteList(regexs: List[Regex]) extends FilterList {

	def accept(url: String): Boolean = {

		@tailrec
		def acceptRec(regexs: List[Regex]): Boolean = regexs match {
			case Nil => false
			case head :: tail =>
				head.findFirstIn(url) match {
					case None => acceptRec(tail)
					case _ => true
				}
		}

		if (regexs.isEmpty)
			true
		else
			acceptRec(regexs)
	}

}

case class BlackList(regexs: List[Regex]) extends FilterList {

	def accept(url: String): Boolean = {

		@tailrec
		def acceptRec(regexs: List[Regex]): Boolean = regexs match {
			case Nil => true
			case head :: tail =>
				head.findFirstIn(url) match {
					case None => acceptRec(tail)
					case _ => false
				}
		}

		if (regexs.isEmpty)
			true
		else
			acceptRec(regexs)
	}
}