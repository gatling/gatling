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

import java.net.{ URI, URISyntaxException }

import scala.collection.mutable

import com.ning.http.util.AsyncHttpProviderUtils
import com.typesafe.scalalogging.slf4j.Logging

import jodd.lagarto.{ EmptyTagVisitor, LagartoParser, Tag }

object HtmlParser extends Logging {

	// TODO parse css content, filter out those with url(), cache and try to apply on DOM
	def getStaticResources(htmlContent: String, documentURI: URI): Seq[String] = {

		// TODO more efficient solution? browser behavior? should this be done here of after resolving absolute urls?
		val rawResources = mutable.LinkedHashSet.empty[String]
		var baseURI: Option[URI] = None

		val lagartoParser = new LagartoParser(htmlContent)

		val visitor = new EmptyTagVisitor {

			private def addAttribute(tag: Tag, attributeName: String) {
				val url = tag.getAttributeValue(attributeName, false)
				if (url != null)
					rawResources += url
			}

			override def script(tag: Tag, body: CharSequence) {
				addAttribute(tag, "src")
			}

			override def tag(tag: Tag) {

				def suffixedCodeBase() = Option(tag.getAttributeValue("codebase", false)).map { cb =>
					if (cb.charAt(cb.size) != '/')
						cb + '/'
					else
						cb
				}

				def prependCodeBase(url: String, codeBase: String) =
					if (url.charAt(0) != 'h')
						codeBase + url
					else
						url

				tag.getName.toLowerCase match {
					case "base" =>
						val baseHref = Option(tag.getAttributeValue("href", false))
						baseURI = try {
							baseHref.map(new URI(_))
						} catch {
							case e: URISyntaxException =>
								logger.debug(s"Malformed baseHref ${baseHref.get}")
								None
						}

					case "link" => addAttribute(tag, "href")

					case "bgsound" => addAttribute(tag, "src")
					case "frame" => addAttribute(tag, "src")
					case "iframe" => addAttribute(tag, "src")
					case "img" => addAttribute(tag, "src")
					case "embed" => addAttribute(tag, "src")
					case "input" => addAttribute(tag, "src") // only if type=image?

					case "body" => addAttribute(tag, "background")

					case "applet" =>
						val code = tag.getAttributeValue("code", false)
						val codeBase = suffixedCodeBase
						val archives = Option(tag.getAttributeValue("archive", false)).map(_.split(",").map(_.trim).toList)

						val appletResources = archives.getOrElse(List(code))
						val appletResourcesUrls = codeBase
							.map(cb => appletResources.map(prependCodeBase(cb, _)))
							.getOrElse(appletResources)

						appletResourcesUrls.foreach(rawResources +=)

					case "object" =>
						val data = tag.getAttributeValue("data", false)
						val codeBase = suffixedCodeBase
						val objectResourceUrl = codeBase
							.map(cb => prependCodeBase(cb, data))
							.getOrElse(data)

						rawResources += objectResourceUrl

					case _ => // TODO: style attribute containing url()
				}
			}
		}

		lagartoParser.parse(visitor)

		val rootURI = baseURI.getOrElse(documentURI)

		rawResources.toSeq.map(AsyncHttpProviderUtils.getRedirectUri(rootURI, _).toString)
	}
}
