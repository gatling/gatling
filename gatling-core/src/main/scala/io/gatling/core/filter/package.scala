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
package io.gatling.core

import scala.annotation.tailrec

package object filter {

	implicit class FilterListWrapper(val filters: List[FilterList]) extends AnyVal {

		def accept(string: String): Boolean = {
			@tailrec
			def acceptRec(string: String, filters: List[FilterList]): Boolean = filters match {
				case Nil => true
				case head :: tail => head.accept(string) && acceptRec(string, tail)
			}

			acceptRec(string, filters)
		}

		def filter(strings: List[String]) = strings.filter(accept)
	}
}
