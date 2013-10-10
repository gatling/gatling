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

import scala.annotation.{ switch, tailrec }
import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import org.jboss.netty.util.internal.ConcurrentHashMap

import io.gatling.core.util.StringHelper.ensureByteCopy

object CssParser {

	val cache: concurrent.Map[String, Seq[(String, String)]] = new ConcurrentHashMap[String, Seq[(String, String)]]

	def extractUrl(string: String, start: Int, end: Int) = {

		@tailrec
		def trimLeft(cur: Int): Int = (string.charAt(cur): @switch) match {
			case ' ' => trimLeft(cur + 1)
			case '\r' => trimLeft(cur + 1)
			case '\n' => trimLeft(cur + 1)
			case ''' => trimLeft(cur + 1)
			case '"' => trimLeft(cur + 1)
			case _ => cur
		}

		@tailrec
		def trimRight(cur: Int): Int = (string.charAt(cur - 1): @switch) match {
			case ' ' => trimRight(cur - 1)
			case '\r' => trimRight(cur - 1)
			case '\n' => trimRight(cur - 1)
			case ''' => trimRight(cur - 1)
			case '"' => trimRight(cur - 1)
			case _ => cur
		}

		val trimmedStart = trimLeft(start)
		val trimmedEnd = trimRight(end)

		ensureByteCopy(string.substring(trimmedStart, trimmedEnd))
	}

	def extractSelectorsAndUrls(cssContent: String): Seq[(String, String)] = {

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

		var selectorsAndUrls = collection.mutable.ArrayBuffer.empty[(String, String)]

		var withinComment = false
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
					if (!withinComment) {
						selectorsEnd = i
					}

				case '}' =>
					if (!withinComment && i < cssContent.length) {
						selectorsStart = i + 2
					}
				case 'u' =>
					if (!withinComment && i < cssContent.length - 7 && cssContent.charAt(i + 1) == 'r' && cssContent.charAt(i + 2) == 'l' && cssContent.charAt(i + 3) == '(') {
						i = i + 3
						urlStart = i + 1
					}

				case ')' =>
					if (!withinComment) {
						val url = extractUrl(cssContent, urlStart, i)
						for (selector <- extractSelectors(selectorsStart, selectorsEnd)) {
							selectorsAndUrls += (selector -> url)
						}
					}

				case _ =>
			}

			i += 1
		}

		selectorsAndUrls
	}
}
