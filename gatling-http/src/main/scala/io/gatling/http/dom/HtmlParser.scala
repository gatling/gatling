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

import scala.collection.{ breakOut, mutable }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.http.util.HttpHelper
import jodd.lagarto.{ EmptyTagVisitor, LagartoParser, Tag }

object HtmlParser extends Logging {

	def getEmbeddedResources(documentURI: URI, htmlContent: String): List[EmbeddedResource] = {

		// FIXME perf? add an index and sort later?
		val rawResources = mutable.LinkedHashMap.empty[String, URI => EmbeddedResource]
		var baseURI: Option[URI] = None

		val lagartoParser = new LagartoParser(htmlContent)

		val visitor = new EmptyTagVisitor {

			def addResource(tag: Tag, attributeName: String, factory: URI => EmbeddedResource) {
				val url = tag.getAttributeValue(attributeName, false)
				if (url != null)
					rawResources += url -> factory
			}

			override def script(tag: Tag, body: CharSequence) {
				addResource(tag, "src", RegularResource)
			}

			override def style(tag: Tag, body: CharSequence) {
				CssParser.extractUrls(body, CssParser.styleImportsUrls).foreach {
					rawResources += _ -> CssResource
				}
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
						tag.getAttributeValue("rel", false) match {
							case "stylesheet" => addResource(tag, "href", CssResource)
							case "icon" => addResource(tag, "href", RegularResource)
							case _ =>
						}

					case "bgsound" => addResource(tag, "src", RegularResource)
					case "img" => addResource(tag, "src", RegularResource)
					case "embed" => addResource(tag, "src", RegularResource)
					case "input" => addResource(tag, "src", RegularResource) // only if type=image?
					case "body" => addResource(tag, "background", RegularResource)

					case "applet" =>
						val code = tag.getAttributeValue("code", false)
						val codeBase = suffixedCodeBase
						val archives = Option(tag.getAttributeValue("archive", false)).map(_.split(",").map(_.trim)(breakOut))

						val appletResources = archives.getOrElse(List(code))
						val appletResourcesUrls = codeBase
							.map(cb => appletResources.map(prependCodeBase(cb, _)))
							.getOrElse(appletResources)
							.map(_ -> RegularResource)
						rawResources ++= appletResourcesUrls

					case "object" =>
						val data = tag.getAttributeValue("data", false)
						val codeBase = suffixedCodeBase
						val objectResourceUrl = codeBase match {
							case Some(cb) => prependCodeBase(cb, data)
							case _ => data
						}
						rawResources += objectResourceUrl -> RegularResource

					case _ =>
						Option(tag.getAttributeValue("style", false)).foreach { style =>
							val styleUrls = CssParser.extractUrls(style, CssParser.inlineStyleImageUrls).map(_ -> RegularResource)
							rawResources ++= styleUrls
						}
				}
			}
		}

		lagartoParser.parse(visitor)

		val rootURI = baseURI.getOrElse(documentURI)

		rawResources.map { case (url, factory) => HttpHelper.resolveFromURISilently(rootURI, url).map(factory) }.flatten.toList
	}
}
