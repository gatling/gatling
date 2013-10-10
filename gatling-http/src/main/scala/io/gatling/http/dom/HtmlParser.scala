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

	val inlineStyleUrl = """url\((.*)\)""".r

	def getEmbeddedResources(documentURI: URI, htmlContent: String): Seq[EmbeddedResource] = {

		// TODO more efficient solution? browser behavior? should this be done here of after resolving absolute urls?
		val rawResources = mutable.LinkedHashSet.empty[EmbeddedResource]
		var baseURI: Option[URI] = None

		val lagartoParser = new LagartoParser(htmlContent)

		val visitor = new EmptyTagVisitor {

			def addResource(tag: Tag, attributeName: String, resType: EmbeddedResourceType = Regular) {
				val url = tag.getAttributeValue(attributeName, false)
				if (url != null)
					rawResources += EmbeddedResource(url, resType)
			}

			override def script(tag: Tag, body: CharSequence) {
				addResource(tag, "src")
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

					case "link" =>
						val rel = tag.getAttributeValue("rel", false)
						if (rel == "stylesheet")
							addResource(tag, "href", Css)
						else if (rel == "icon")
							addResource(tag, "href")

					case "bgsound" => addResource(tag, "src")
					case "frame" => addResource(tag, "src", Html)
					case "iframe" => addResource(tag, "src", Html)
					case "img" => addResource(tag, "src")
					case "embed" => addResource(tag, "src")
					case "input" => addResource(tag, "src") // only if type=image?

					case "body" => addResource(tag, "background")

					case "applet" =>
						val code = tag.getAttributeValue("code", false)
						val codeBase = suffixedCodeBase
						val archives = Option(tag.getAttributeValue("archive", false)).map(_.split(",").map(_.trim).toList)

						val appletResources = archives.getOrElse(List(code))
						val appletResourcesUrls = codeBase
							.map(cb => appletResources.map(prependCodeBase(cb, _)))
							.getOrElse(appletResources)

						appletResourcesUrls.foreach(rawResources += EmbeddedResource(_))

					case "object" =>
						val data = tag.getAttributeValue("data", false)
						val codeBase = suffixedCodeBase
						val objectResourceUrl = codeBase
							.map(cb => prependCodeBase(cb, data))
							.getOrElse(data)

						rawResources += EmbeddedResource(objectResourceUrl)

					case _ =>
						Option(tag.getAttributeValue("style", false)).foreach { style =>
							inlineStyleUrl.findAllIn(style).matchData.foreach { m =>
								val raw = m.group(1)
								val url = CssParser.extractUrl(raw, 0, raw.length)
								rawResources += EmbeddedResource(url)
							}
						}
				}
			}
		}

		lagartoParser.parse(visitor)

		val rootURI = baseURI.getOrElse(documentURI)

		rawResources.toSeq.map { res =>
			res.copy(url = AsyncHttpProviderUtils.getRedirectUri(rootURI, res.url).toString)
		}
	}
}
