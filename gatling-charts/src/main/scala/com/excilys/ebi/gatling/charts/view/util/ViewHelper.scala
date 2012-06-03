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
package com.excilys.ebi.gatling.charts.view.util

object ViewHelper {

	/**
	 * @param n ordinal number to add a suffix to
	 */
	def ordinalNumberSuffix(n: Long) = {
		n % 10 match {
			case _ if (11 to 13) contains n % 100 => "th"
			case 1 => "st"
			case 2 => "nd"
			case 3 => "rd"
			case _ => "th"
		}
	}
}