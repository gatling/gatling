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
package com.excilys.ebi.gatling.core.util

import java.util.ResourceBundle

import scala.collection.JavaConversions.enumerationAsScalaIterator

object HtmlHelper {

	val ENTITIES = ResourceBundle.getBundle("html-entities")

	val CHAR_TO_HTML_ENTITIES: Map[Char, String] = ENTITIES.getKeys.map { entityName => (ENTITIES.getString(entityName).toInt.toChar, "&" + entityName + ";") }.toMap

	val HTML_ENTITIES_TO_CHAR: Map[String, Char] = CHAR_TO_HTML_ENTITIES.map(_.swap)

	def htmlEntityToInt(entity: String, default: Int): Int = HTML_ENTITIES_TO_CHAR.get(entity).map(_.toInt).getOrElse(default)

	def htmlEscape(string: String): String = {
		def charToHtmlEntity(char: Char): String = CHAR_TO_HTML_ENTITIES.get(char).getOrElse(char.toString)

		string.toList.map(charToHtmlEntity).mkString
	}

	def formatToJavascriptVar(string: String) = {
		val fileName = FileHelper.formatToFilename(string)
		val firstChar = fileName.charAt(0)

		if (firstChar >= 'a' && firstChar <= 'z')
			fileName
		else
			"_" + fileName
	}
}