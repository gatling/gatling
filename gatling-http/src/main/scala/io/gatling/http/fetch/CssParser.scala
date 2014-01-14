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
package io.gatling.http.fetch

import java.net.URI

import scala.annotation.{ switch, tailrec }
import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.util.matching.Regex

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.util.StringHelper.ensureCharCopy
import io.gatling.http.util.HttpHelper

// FIXME Would it be more efficient to work with Array[Char] instead of String?
object CssParser extends StrictLogging {

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
	val atImportChars = "@import".toCharArray
	val urlStartChars = "url(".toCharArray

	def extractUrl(string: String, start: Int, end: Int): Option[String] = {

		var protectChar: Option[Char] = None
		var broken = false

		@tailrec
		def trimLeft(cur: Int): Int = (string.charAt(cur): @switch) match {
			case ' ' | '\r' | '\n' => trimLeft(cur + 1)
			case ''' =>
				protectChar match {
					case None =>
						protectChar = singleQuoteEscapeChar
						trimLeft(cur + 1)
					case _ =>
						broken = true
						cur

				}
			case '"' =>
				protectChar match {
					case None =>
						protectChar = doubleQuoteEscapeChar
						trimLeft(cur + 1)
					case _ =>
						broken = true
						cur
				}
			case _ => cur
		}

		@tailrec
		def trimRight(cur: Int): Int = (string.charAt(cur - 1): @switch) match {
			case ' ' | '\r' | '\n' => trimRight(cur - 1)
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
			Some(ensureCharCopy(string.substring(trimmedStart, trimmedEnd)))
		} else {
			logger.info(s"css url broken between positions ${string.substring(trimmedStart, trimmedEnd)}")
			None
		}
	}

	def extractResources(cssURI: URI, cssContent: String): List[EmbeddedResource] = {

		val resources = collection.mutable.ArrayBuffer.empty[EmbeddedResource]

		var withinComment = false
		var withinImport = false
		var withinUrl = false
		var urlStart = 0

		def charsMatch(i: Int, chars: Array[Char]): Boolean = {

			@tailrec
			def charsMatchRec(j: Int): Boolean = {
				if (j == chars.length)
					true
				else if (cssContent.charAt(i + j) != chars(j))
					false
				else
					charsMatchRec(j + 1)

			}

			i < cssContent.length - chars.length && charsMatchRec(1)
		}

		var i = 0
		while (i < cssContent.length) {

			(cssContent.charAt(i): @switch) match {
				case '/' =>
					if (i < cssContent.length - 1 &&
						cssContent.charAt(i + 1) == '*') {
						withinComment = true
						i += 1

					} else if (i > 0 &&
						cssContent.charAt(i - 1) == '*') {
						withinComment = false
					}

				case '@' if !withinComment && charsMatch(i, atImportChars) => {
					withinImport = true
					i = i + "@import".length
				}

				case 'u' if !withinComment && withinImport && charsMatch(i, urlStartChars) => {
					i = i + urlStartChars.length
					urlStart = i
					withinUrl = true
				}

				case ')' if !withinComment && withinUrl =>
					for {
						url <- extractUrl(cssContent, urlStart, i)
						absoluteUri <- HttpHelper.resolveFromURISilently(cssURI, url)
					} {
						resources += CssResource(absoluteUri)
						withinUrl = false
						withinImport = false
					}

				case _ =>
			}

			i += 1
		}

		resources.toList
	}
}
