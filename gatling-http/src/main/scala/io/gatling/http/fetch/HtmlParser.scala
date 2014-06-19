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
import jodd.lagarto.dom.HtmlCCommentExpressionMatcher

import scala.collection.{ breakOut, mutable }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.http.util.HttpHelper
import jodd.lagarto.{ TagUtil, TagType, EmptyTagVisitor, Tag }

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

case class HtmlResources(rawResources: Seq[RawResource], baseURI: Option[URI])

object HtmlParser {
  val AppletTagName = "applet".toCharArray
  val BaseTagName = "base".toCharArray
  val BgsoundTagName = "bgsound".toCharArray
  val BodyTagName = "body".toCharArray
  val EmbedTagName = "embed".toCharArray
  val ImgTagName = "img".toCharArray
  val InputTagName = "input".toCharArray
  val LinkTagName = "link".toCharArray
  val ObjectTagName = "object".toCharArray
  val StyleTagName = "style".toCharArray

  val ArchiveAttribute = "archive".toCharArray
  val BackgroungAttribute = "background".toCharArray
  val CodeAttribute = "code".toCharArray
  val CodeBaseAttribute = "codebase".toCharArray
  val DataAttribute = "data".toCharArray
  val HrefAttribute = "href".toCharArray
  val IconAttributeName = "icon".toCharArray
  val RelAttribute = "rel".toCharArray
  val SrcAttribute = "src".toCharArray
  val StyleAttribute = StyleTagName
  val StylesheetAttributeName = "stylesheet".toCharArray

  val IE_VERSION_WITHOUT_CC = 10

  def getIeVersion(userAgent: Option[UserAgent]): Option[Float] = {
    userAgent match {
      case Some(agent) =>
        if (agent.version < IE_VERSION_WITHOUT_CC) Some(agent.version)
        else None

      case None => None
    }
  }
}

class HtmlParser extends StrictLogging {

  import HtmlParser._

  var inStyle = false

  def parseHtml(htmlContent: Array[Char], userAgent: Option[UserAgent]): HtmlResources = {

    var baseURI: Option[URI] = None
    val rawResources = mutable.ArrayBuffer.empty[RawResource]
    val conditionalCommentsMatcher = new HtmlCCommentExpressionMatcher()
    val matchMethod = conditionalCommentsMatcher.getClass.getDeclaredMethod("match", java.lang.Float.TYPE, classOf[String])
    matchMethod.setAccessible(true)
    val ieVersion = getIeVersion(userAgent)

    val visitor = new EmptyTagVisitor {
      var inHiddenCommentStack = List(false)

      def addResource(tag: Tag, attributeName: Array[Char], factory: String => RawResource): Unit =
        Option(tag.getAttributeValue(attributeName)).foreach { url =>
          rawResources += factory(url.toString)
        }

      override def script(tag: Tag, body: CharSequence): Unit =
        if (!isInHiddenComment) {
          addResource(tag, SrcAttribute, RegularRawResource)
        }

      override def text(text: CharSequence): Unit = if (inStyle) {
        if (!isInHiddenComment) {
          rawResources ++= CssParser.extractUrls(text, CssParser.StyleImportsUrls).map(CssRawResource)
        }
      }

      private def isInHiddenComment = inHiddenCommentStack.head

      override def condComment(expression: CharSequence, isStartingTag: Boolean, isHidden: Boolean, isHiddenEndTag: Boolean) {
        ieVersion match {
          case Some(version) =>
            if (!isStartingTag) {
              inHiddenCommentStack = inHiddenCommentStack.tail
            } else {
              val commentValue = matchMethod.invoke(conditionalCommentsMatcher, version: java.lang.Float, expression.toString).asInstanceOf[Boolean]
              inHiddenCommentStack = (!commentValue) :: inHiddenCommentStack
            }
          case None => inHiddenCommentStack = true :: inHiddenCommentStack
        }
      }

      override def tag(tag: Tag): Unit = {

          def codeBase() = Option(tag.getAttributeValue(CodeBaseAttribute))

          def prependCodeBase(codeBase: CharSequence, url: String) =
            if (url.startsWith("http"))
              url
            else if (codeBase.charAt(codeBase.length()) != '/')
              codeBase + "/" + url
            else
              codeBase + url

          def processTag {
            tag.getType match {

              case TagType.START | TagType.SELF_CLOSING =>

                if (tag.isRawTag && tag.nameEquals(StyleTagName)) {
                  inStyle = true

                } else if (tag.nameEquals(BaseTagName)) {
                  val baseHref = Option(tag.getAttributeValue(HrefAttribute))
                  baseURI = baseHref.flatMap { bh =>
                    try {
                      baseHref.map(bh => new URI(bh.toString))
                    } catch {
                      case e: URISyntaxException =>
                        logger.debug(s"Malformed baseHref ${baseHref.get}")
                        None
                    }
                  }

                } else if (tag.nameEquals(LinkTagName)) {
                  val rel = tag.getAttributeValue(RelAttribute)

                  if (TagUtil.equalsToLowercase(rel, StylesheetAttributeName)) {
                    addResource(tag, HrefAttribute, CssRawResource)
                  } else if (TagUtil.equalsToLowercase(rel, IconAttributeName)) {
                    addResource(tag, HrefAttribute, RegularRawResource)
                  }

                } else if (tag.nameEquals(ImgTagName) ||
                  tag.nameEquals(BgsoundTagName) ||
                  tag.nameEquals(EmbedTagName) ||
                  tag.nameEquals(InputTagName)) {

                  addResource(tag, SrcAttribute, RegularRawResource)

                } else if (tag.nameEquals(BodyTagName)) {
                  addResource(tag, BackgroungAttribute, RegularRawResource)

                } else if (tag.nameEquals(AppletTagName)) {
                  val code = tag.getAttributeValue(CodeAttribute).toString
                  val archives = Option(tag.getAttributeValue(ArchiveAttribute).toString).map(_.split(",").map(_.trim)(breakOut))

                  val appletResources = archives.getOrElse(List(code)).iterator
                  val appletResourcesUrls = codeBase() match {
                    case Some(cb) => appletResources.map(prependCodeBase(cb, _))
                    case _        => appletResources
                  }
                  rawResources ++= appletResourcesUrls.map(RegularRawResource)

                } else if (tag.nameEquals(ObjectTagName)) {
                  val data = tag.getAttributeValue(DataAttribute).toString
                  val objectResourceUrl = codeBase() match {
                    case Some(cb) => prependCodeBase(cb, data)
                    case _        => data
                  }
                  rawResources += RegularRawResource(objectResourceUrl)

                } else {
                  Option(tag.getAttributeValue(StyleAttribute)).foreach { style =>
                    val styleUrls = CssParser.extractUrls(style, CssParser.InlineStyleImageUrls).map(RegularRawResource)
                    rawResources ++= styleUrls
                  }
                }

              case TagType.END =>
                if (inStyle && tag.nameEquals(StyleTagName)) {
                  inStyle = false
                }

              case _ =>
            }
          }

        if (!isInHiddenComment) {
          processTag
        }
      }
    }

    Jodd.newLagartoParser(htmlContent, ieVersion).parse(visitor)
    HtmlResources(rawResources, baseURI)
  }

  def getEmbeddedResources(documentURI: URI, htmlContent: Array[Char], userAgent: Option[UserAgent]): List[EmbeddedResource] = {

    val htmlResources = parseHtml(htmlContent, userAgent)

    val rootURI = htmlResources.baseURI.getOrElse(documentURI)

    htmlResources.rawResources
      .distinct
      .iterator
      .filterNot(res => res.rawUrl.isEmpty || res.rawUrl.charAt(0) == '#' || res.rawUrl.startsWith("data:"))
      .flatMap(_.toEmbeddedResource(rootURI))
      .toList
  }
}
