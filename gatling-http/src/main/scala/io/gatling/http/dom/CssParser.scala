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
package io.gatling.http.dom

import java.net.URI

import scala.annotation.{ switch, tailrec }
import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.util.matching.Regex

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.util.StringHelper.ensureByteCopy
import io.gatling.http.util.HttpHelper

case class CssContent(importRules: List[URI], styleRules: Seq[StyleRule])
case class StyleRule(selector: String, uri: URI)

object CssParser extends Logging {

	val inlineStyleImageUrls = """url\((.*)\)""".r
	val styleImportsUrls = """@import.* url\((.*)\)""".r

	def extractUrls(string: CharSequence, regex: Regex): Iterator[String] = {
		regex.findAllIn(string).matchData.map { m =>
			val raw = m.group(1)
			extractUrl(raw, 0, raw.length)
		}.flatten
	}

	val singleQuoteEscapeChar = Some(''')
	val doubleQuoteEscapeChar = Some('"')

	def extractUrl(string: String, start: Int, end: Int) = {

		var protectChar: Option[Char] = None
		var broken = false

		@tailrec
		def trimLeft(cur: Int): Int = (string.charAt(cur): @switch) match {
			case ' ' => trimLeft(cur + 1)
			case '\r' => trimLeft(cur + 1)
			case '\n' => trimLeft(cur + 1)
			case ''' =>
				protectChar match {
					case None =>
						protectChar = singleQuoteEscapeChar
						trimLeft(cur + 1)
					case Some(_) =>
						broken = true
						cur

				}
			case '"' =>
				protectChar match {
					case None =>
						protectChar = doubleQuoteEscapeChar
						trimLeft(cur + 1)
					case Some(_) =>
						broken = true
						cur
				}
			case _ => cur
		}

		@tailrec
		def trimRight(cur: Int): Int = (string.charAt(cur - 1): @switch) match {
			case ' ' => trimRight(cur - 1)
			case '\r' => trimRight(cur - 1)
			case '\n' => trimRight(cur - 1)
			case ''' => protectChar match {
				case `singleQuoteEscapeChar` =>
					trimRight(cur - 1)
				case _ =>
					broken = true
					cur
			}
			case '"' => protectChar match {
				case `doubleQuoteEscapeChar` =>
					trimRight(cur - 1)
				case _ =>
					broken = true
					cur
			}
			case _ => cur
		}

		val trimmedStart = trimLeft(start)
		val trimmedEnd = trimRight(end)

		if (!broken) {
			Some(ensureByteCopy(string.substring(trimmedStart, trimmedEnd)))
		} else {
			logger.info(s"css url broken between positions $trimmedStart and $trimmedEnd")
			None
		}
	}

	def extractRules(cssURI: URI, cssContent: String): CssContent = {

		def extractSelectors(start: Int, end: Int): Array[String] = {

			var withinComment = false
			val filtered = new Array[Char](end - start + 1)
			var filteredPosition = 0

			var i = start
			while (i < end) {

				val curr = cssContent.charAt(i)

				(curr: @switch) match {

					case '/' =>
						if (cssContent.charAt(i + 1) == '*') {
							withinComment = true
							i += 1
						} else if (i > 0 && cssContent.charAt(i - 1) == '*') {
							withinComment = false
						}

					case '\r' =>
					case '\n' =>

					case _ =>
						if (!withinComment) {
							filtered(filteredPosition) = curr
							filteredPosition += 1
						}
				}

				i += 1
			}

			new String(filtered, 0, filteredPosition).split(",").map(_.trim)
		}

		var importRules = collection.mutable.ArrayBuffer.empty[URI]
		var styleRules = collection.mutable.ArrayBuffer.empty[StyleRule]

		var withinComment = false
		var withinImport = false
		var selectorsStart = 0
		var selectorsEnd = 0
		var urlStart = 0

		var i = 0
		while (i < cssContent.length) {

			(cssContent.charAt(i): @switch) match {
				case '/' =>
					if (cssContent.charAt(i + 1) == '*') {
						withinComment = true
						i += 1

					} else if (i > 0 && cssContent.charAt(i - 1) == '*') {
						withinComment = false
					}

				case '{' =>
					if (!withinComment)
						selectorsEnd = i

				case '}' =>
					if (!withinComment && i < cssContent.length)
						selectorsStart = i + 2

				case '@' =>
					if (!withinComment &&
						cssContent.charAt(i + 1) == 'i' &&
						cssContent.charAt(i + 2) == 'm' &&
						cssContent.charAt(i + 3) == 'p' &&
						cssContent.charAt(i + 4) == 'o' &&
						cssContent.charAt(i + 5) == 'r' &&
						cssContent.charAt(i + 6) == 't') {

						withinImport = true
						i = i + 7
					}

				case 'u' =>
					if (!withinComment && i < cssContent.length - 7 &&
						cssContent.charAt(i + 1) == 'r' &&
						cssContent.charAt(i + 2) == 'l' &&
						cssContent.charAt(i + 3) == '(') {

						i = i + 3
						urlStart = i + 1
					}

				case ')' if !withinComment =>
					for {
						url <- extractUrl(cssContent, urlStart, i)
						absoluteUri <- HttpHelper.resolveFromURISilently(cssURI, url)
					} {
						if (withinImport) {
							importRules += absoluteUri
							withinImport = false

						} else {
							for (selector <- extractSelectors(selectorsStart, selectorsEnd)) {
								styleRules += StyleRule(selector, absoluteUri)
							}
						}
					}

				case _ =>
			}

			i += 1
		}

		CssContent(importRules.toList, styleRules)
	}
}
