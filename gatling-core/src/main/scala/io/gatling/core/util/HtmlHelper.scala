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
package io.gatling.core.util

import java.util.ResourceBundle

import scala.collection.JavaConversions.enumerationAsScalaIterator

import io.gatling.core.util.FileHelper.FileRichString

object HtmlHelper {

	val entities = ResourceBundle.getBundle("html-entities")

	val charToHtmlEntities: Map[Char, String] = entities.getKeys.map { entityName => (entities.getString(entityName).toInt.toChar, s"&$entityName;") }.toMap

	val htmlEntitiesToChar: Map[String, Char] = charToHtmlEntities.map(_.swap)

	implicit class HtmlRichString(val string: String) extends AnyVal {

		def htmlEscape: String = {
			def charToHtmlEntity(char: Char): String = charToHtmlEntities.get(char).getOrElse(char.toString)

			string.toCharArray.iterator.map(charToHtmlEntity).mkString
		}
	}

	// used in VTD-XML extension
	def htmlEntityToInt(entity: String, default: Int): Int = htmlEntitiesToChar.get(entity).map(_.toInt).getOrElse(default)
}