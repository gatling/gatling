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

import java.net.{ URI, URISyntaxException }

import scala.collection.{ breakOut, mutable }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.http.util.HttpHelper
import jodd.lagarto.{ EmptyTagVisitor, LagartoParser, Tag }

sealed abstract class RawResource {
  def rawUrl: String
  def uri(rootURI: URI): Option[URI] = HttpHelper.resolveFromURISilently(rootURI, rawUrl)
  def toEmbeddedResource(rootURI: URI): Option[EmbeddedResource]
}
case class CssRawResource(rawUrl: String) extends RawResource {
  def toEmbeddedResource(rootURI: URI): Option[EmbeddedResource] = uri(rootURI).map(CssResource)
}
case class RegularRawResource(rawUrl: String) extends RawResource {
  def toEmbeddedResource(rootURI: URI): Option[EmbeddedResource] = uri(rootURI).map(RegularResource)
}

object HtmlParser extends StrictLogging {

  def getEmbeddedResources(documentURI: URI, htmlContent: Array[Char]): List[EmbeddedResource] = {

    val rawResources = mutable.ArrayBuffer.empty[RawResource]
    var baseURI: Option[URI] = None

    val lagartoParser = new LagartoParser(htmlContent)

    val visitor = new EmptyTagVisitor {

      def addResource(tag: Tag, attributeName: String, factory: String => RawResource) {
        val url = tag.getAttributeValue(attributeName, false)
        if (url != null)
          rawResources += factory(url)
      }

      override def script(tag: Tag, body: CharSequence) {
        addResource(tag, "src", RegularRawResource)
      }

      override def style(tag: Tag, body: CharSequence) {
        rawResources ++= CssParser.extractUrls(body, CssParser.styleImportsUrls).map(CssRawResource)
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
              case "stylesheet" => addResource(tag, "href", CssRawResource)
              case "icon"       => addResource(tag, "href", RegularRawResource)
              case _            =>
            }

          case "bgsound" => addResource(tag, "src", RegularRawResource)
          case "img"     => addResource(tag, "src", RegularRawResource)
          case "embed"   => addResource(tag, "src", RegularRawResource)
          case "input"   => addResource(tag, "src", RegularRawResource) // only if type=image?
          case "body"    => addResource(tag, "background", RegularRawResource)

          case "applet" =>
            val code = tag.getAttributeValue("code", false)
            val archives = Option(tag.getAttributeValue("archive", false)).map(_.split(",").map(_.trim)(breakOut))

            val appletResources = archives.getOrElse(List(code)).iterator
            val appletResourcesUrls = suffixedCodeBase() match {
              case Some(cb) => appletResources.map(prependCodeBase(cb, _))
              case None     => appletResources
            }
            rawResources ++= appletResourcesUrls.map(RegularRawResource)

          case "object" =>
            val data = tag.getAttributeValue("data", false)
            val objectResourceUrl = suffixedCodeBase() match {
              case Some(cb) => prependCodeBase(cb, data)
              case _        => data
            }
            rawResources += RegularRawResource(objectResourceUrl)

          case _ =>
            Option(tag.getAttributeValue("style", false)).foreach { style =>
              val styleUrls = CssParser.extractUrls(style, CssParser.inlineStyleImageUrls).map(RegularRawResource)
              rawResources ++= styleUrls
            }
        }
      }
    }

    lagartoParser.parse(visitor)

    val rootURI = baseURI.getOrElse(documentURI)

    rawResources
      .distinct
      .flatMap(_.toEmbeddedResource(rootURI))
      .toList
  }
}
