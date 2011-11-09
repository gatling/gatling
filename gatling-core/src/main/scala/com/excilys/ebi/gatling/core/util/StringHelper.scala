/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.util
import org.slf4j.helpers.MessageFormatter
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import java.util.regex.Pattern
import java.text.Normalizer

/**
 * This object groups all utilities for strings
 */
object StringHelper extends Logging {

	val EMPTY = ""

	val jdk6Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

	/**
	 * Returns a string with all {} replaced by specified values as in :
	 * {{{
	 * //context has attribute "key" set to "interpolation"
	 * interpolate("This is an {}", "key") => "This is an interpolation"
	 * }}}
	 *
	 * @param context the context in which values must be taken
	 * @param stringToFormat the string to be formatted
	 * @param interpolations the keys of the values that will be inserted in stringToFormat
	 * @return the completed string
	 */
	def interpolateString(context: Context, stringToFormat: String, interpolations: Seq[String]) = {

		interpolations.size match {
			case 0 => stringToFormat
			case 1 => MessageFormatter.format(stringToFormat, context.getAttribute(interpolations(0))).getMessage
			case 2 => MessageFormatter.format(stringToFormat, context.getAttribute(interpolations(0)), context.getAttribute(interpolations(1))).getMessage
			case _ => {
				val interpolationsFromContext: Seq[String] = for (interpolation <- interpolations) yield context.getAttribute(interpolation).toString
				MessageFormatter.arrayFormat(stringToFormat, interpolationsFromContext.toArray).getMessage
			}
		}
	}

	/**
	 * Method used in scenarios to interpolate strings from context
	 *
	 * @param stringToFormat the string to be formatted
	 * @param interpolations the keys of the values that will be inserted in stringToFormat
	 * @return the completed string
	 */
	def interpolate(stringToFormat: String, interpolations: String*) = (c: Context) => interpolateString(c, stringToFormat, interpolations)

	/**
	 * Method that strips all accents from a string
	 */
	def stripAccents(string: String) = {
		val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
		jdk6Pattern.matcher(normalized).replaceAll(EMPTY);
	}
}