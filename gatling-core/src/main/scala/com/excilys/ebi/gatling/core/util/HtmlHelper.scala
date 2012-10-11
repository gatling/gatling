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
package com.excilys.ebi.gatling.core.util

import java.util.ResourceBundle

import scala.collection.JavaConversions.enumerationAsScalaIterator

object HtmlHelper {

	val ENTITIES = ResourceBundle.getBundle("html-entities")

	val CHAR_TO_HTML_ENTITIES: Map[Int, String] = ENTITIES.getKeys.map { entityName => (ENTITIES.getString(entityName).toInt, entityName) }.toMap

	val HTML_ENTITIES_TO_CHAR = ENTITIES.getKeys.map { entityName => (entityName, ENTITIES.getString(entityName).toInt) }.toMap

	def htmlEntityToChar(entity: String): Option[Int] = HTML_ENTITIES_TO_CHAR.get(entity)

	def htmlEntityToChar(entity: String, default: Int): Int = HTML_ENTITIES_TO_CHAR.get(entity).getOrElse(default)

	def charToHtmlEntity(entity: Int): Option[String] = CHAR_TO_HTML_ENTITIES.get(entity)

	def htmlEscape(string: String): String = {

		(for (i <- 0 until string.length) yield string.charAt(i))
			.foldLeft(new StringBuilder) { (builder, char) =>
				charToHtmlEntity(char) match {
					case Some(escaped) => builder.append(escaped)
					case _ => builder.append(char)
				}
			}.toString
	}
}