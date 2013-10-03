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

import scala.collection.mutable

import jodd.lagarto.{ EmptyTagVisitor, LagartoParser, Tag }

object HtmlParser {

	// TODO parse css content, filter out those with url(), cache and try to apply on DOM
	def getStaticResources(htmlContent: String): Seq[String] = {

		// TODO more efficient solution? browser behavior? should this be done here of after resolving absolute urls?
		val resources = mutable.LinkedHashSet.empty[String]

		val lagartoParser = new LagartoParser(htmlContent)

		val visitor = new EmptyTagVisitor {

			private def addAttribute(tag: Tag, attributeName: String) {
				val url = tag.getAttributeValue(attributeName, false)
				if (url != null)
					resources += url
			}

			override def script(tag: Tag, body: CharSequence) {
				addAttribute(tag, "src")
			}

			override def tag(tag: Tag) {

				tag.getName.toLowerCase match {
					case "base" => addAttribute(tag, "href")
					case "link" => addAttribute(tag, "href")

					case "bgsound" => addAttribute(tag, "src")
					case "frame" => addAttribute(tag, "src")
					case "iframe" => addAttribute(tag, "src")
					case "img" => addAttribute(tag, "src")
					case "input" => addAttribute(tag, "src") // only if type=image?

					case "body" => addAttribute(tag, "background")
					case _ => // TODO: applet, embed, object, style attribute containing url()
				}
			}
		}

		lagartoParser.parse(visitor)

		resources.toSeq
	}
}
