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

import io.gatling.core.check.extractor.css.Jodd

import scala.collection.{ breakOut, mutable }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.http.util.HttpHelper
import jodd.lagarto.{EmptyTagVisitor, Tag}

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

  case class HtmlResources(rawResources: Seq[RawResource], baseURI: Option[URI])

  def parseHtml(htmlContent: Array[Char]): HtmlResources = {

    var baseURI: Option[URI] = None
    val rawResources = mutable.ArrayBuffer.empty[RawResource]

    val visitor = new EmptyTagVisitor {

      def addResource(tag: Tag, attributeName: String, factory: String => RawResource): Unit = {
        val url = tag.getAttributeValue(attributeName, false)
        if (url != null)
          rawResources += factory(url)
      }

      override def script(tag: Tag, body: CharSequence): Unit =
        addResource(tag, "src", RegularRawResource)

      override def style(tag: Tag, body: CharSequence): Unit =
        rawResources ++= CssParser.extractUrls(body, CssParser.StyleImportsUrls).map(CssRawResource)

      override def tag(tag: Tag): Unit = {

        def codeBase() = Option(tag.getAttributeValue("codebase", false))

        def prependCodeBase(url: String, codeBase: String) =
          if (url.startsWith("http"))
            url
          else if (codeBase.charAt(codeBase.size) != '/')
            codeBase + '/' + url
          else
            codeBase + url

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
              case "icon" => addResource(tag, "href", RegularRawResource)
              case _ =>
            }

          case "bgsound" | "img" | "embed" | "input" => addResource(tag, "src", RegularRawResource)
          case "body" => addResource(tag, "background", RegularRawResource)

          case "applet" =>
            val code = tag.getAttributeValue("code", false)
            val archives = Option(tag.getAttributeValue("archive", false)).map(_.split(",").map(_.trim)(breakOut))

            val appletResources = archives.getOrElse(List(code)).iterator
            val appletResourcesUrls = codeBase() match {
              case Some(cb) => appletResources.map(prependCodeBase(cb, _))
              case None => appletResources
            }
            rawResources ++= appletResourcesUrls.map(RegularRawResource)

          case "object" =>
            val data = tag.getAttributeValue("data", false)
            val objectResourceUrl = codeBase() match {
              case Some(cb) => prependCodeBase(cb, data)
              case _ => data
            }
            rawResources += RegularRawResource(objectResourceUrl)

          case _ =>
            Option(tag.getAttributeValue("style", false)).foreach { style =>
              val styleUrls = CssParser.extractUrls(style, CssParser.InlineStyleImageUrls).map(RegularRawResource)
              rawResources ++= styleUrls
            }
        }
      }
    }

    Jodd.newLagartoParser(htmlContent).parse(visitor)
    HtmlResources(rawResources, baseURI)
  }

  def getEmbeddedResources(documentURI: URI, htmlContent: Array[Char]): List[EmbeddedResource] = {

    val htmlResources = parseHtml(htmlContent)

    val rootURI = htmlResources.baseURI.getOrElse(documentURI)

    htmlResources.rawResources
      .distinct
      .iterator
      .filterNot(res => res.rawUrl.isEmpty || res.rawUrl.charAt(0) == '#' || res.rawUrl.startsWith("data:"))
      .flatMap(_.toEmbeddedResource(rootURI))
      .toList
  }
}
