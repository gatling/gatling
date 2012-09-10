package com.excilys.ebi.gatling.core.util

import java.util.ResourceBundle

import scala.collection.JavaConversions._

object HtmlHelper {

	val ENTITIES = ResourceBundle.getBundle("html-entities")

	val CHAR_TO_HTML_ENTITIES: Map[Int, String] = ENTITIES.getKeys.map { entityName => (ENTITIES.getString(entityName).toInt, entityName) }.toMap

	val HTML_ENTITIES_TO_CHAR = ENTITIES.getKeys.map { entityName => (entityName, ENTITIES.getString(entityName).toInt) }.toMap

	def htmlEntityToChar(entity: String): Option[Int] = HTML_ENTITIES_TO_CHAR.get(entity)
	
	def htmlEntityToChar(entity: String, default: Int): Int = HTML_ENTITIES_TO_CHAR.get(entity).getOrElse(default)

	def charToHtmlEntity(entity: Int): Option[String] = CHAR_TO_HTML_ENTITIES.get(entity)

	def htmlEscape(string: String): String = {

		val escapedChars = for (i <- 0 until string.length) yield {
			val nonEscaped = string.charAt(i).toInt
			charToHtmlEntity(nonEscaped).getOrElse(nonEscaped)
		}

		escapedChars.foldLeft(new StringBuilder)((builder, char) => builder.append(char)).toString
	}
}