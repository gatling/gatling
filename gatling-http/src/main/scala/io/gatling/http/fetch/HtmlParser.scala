/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.fetch

import scala.collection.{ breakOut, mutable }
import scala.util.control.NonFatal

import io.gatling.core.check.extractor.css.Jodd
import io.gatling.http.util.HttpHelper

import com.typesafe.scalalogging.StrictLogging
import jodd.lagarto.{ EmptyTagVisitor, Tag, TagType }
import jodd.lagarto.dom.HtmlCCommentExpressionMatcher
import jodd.util.CharSequenceUtil
import org.asynchttpclient.uri.Uri

sealed abstract class RawResource {
  def rawUrl: String
  def uri(rootURI: Uri): Option[Uri] = HttpHelper.resolveFromUriSilently(rootURI, rawUrl)
  def toEmbeddedResource(rootURI: Uri): Option[EmbeddedResource]
}
case class CssRawResource(rawUrl: String) extends RawResource {
  def toEmbeddedResource(rootURI: Uri): Option[EmbeddedResource] = uri(rootURI).map(CssResource)
}
case class RegularRawResource(rawUrl: String) extends RawResource {
  def toEmbeddedResource(rootURI: Uri): Option[EmbeddedResource] = uri(rootURI).map(RegularResource)
}

case class HtmlResources(rawResources: Seq[RawResource], base: Option[String])

object HtmlParser extends StrictLogging {
  private val AppletTagName = "applet"
  private val BaseTagName = "base"
  private val BgsoundTagName = "bgsound"
  private val BodyTagName = "body"
  private val EmbedTagName = "embed"
  private val ImgTagName = "img"
  private val InputTagName = "input"
  private val LinkTagName = "link"
  private val ObjectTagName = "object"
  private val StyleTagName = "style"

  private val ArchiveAttribute = "archive"
  private val BackgroundAttribute = "background"
  private val CodeAttribute = "code"
  private val CodeBaseAttribute = "codebase"
  private val DataAttribute = "data"
  private val HrefAttribute = "href"
  private val IconAttributeName = "icon"
  private val ShortcutIconAttributeName = "shortcut icon"
  private val RelAttribute = "rel"
  private val SrcAttribute = "src"
  private val StyleAttribute = StyleTagName
  private val StylesheetAttributeName = "stylesheet"

  def logException(htmlContent: Array[Char], e: Throwable): Unit =
    if (logger.underlying.isDebugEnabled)
      logger.debug(s"""HTML parser crashed, there's a chance your page wasn't proper HTML:
>>>>>>>>>>>>>>>>>>>>>>>
${new String(htmlContent)}
<<<<<<<<<<<<<<<<<<<<<<<""", e)
    else
      logger.error(s"HTML parser crashed: ${e.getMessage}, there's a chance your page wasn't proper HTML, enable debug on 'io.gatling.http.fetch' logger to get the HTML content", e)
}

class HtmlParser extends StrictLogging {

  import HtmlParser._

  var inStyle = false

  private def parseHtml(htmlContent: Array[Char], userAgent: Option[UserAgent]): HtmlResources = {

    var base: Option[String] = None
    val rawResources = mutable.ArrayBuffer.empty[RawResource]
    val conditionalCommentsMatcher = new HtmlCCommentExpressionMatcher()
    val ieVersion = userAgent.map(_.version)

    val visitor = new EmptyTagVisitor {
      var inHiddenCommentStack = List(false)

      def addResource(tag: Tag, attributeName: String, factory: String => RawResource): Unit =
        Option(tag.getAttributeValue(attributeName)).foreach { url =>
          rawResources += factory(url.toString)
        }

      override def script(tag: Tag, body: CharSequence): Unit =
        if (!isInHiddenComment)
          addResource(tag, SrcAttribute, RegularRawResource)

      override def text(text: CharSequence): Unit =
        if (inStyle && !isInHiddenComment)
          rawResources ++= CssParser.extractStyleImportsUrls(text).map(CssRawResource)

      private def isInHiddenComment = inHiddenCommentStack.head

      override def condComment(expression: CharSequence, isStartingTag: Boolean, isHidden: Boolean, isHiddenEndTag: Boolean): Unit =
        ieVersion match {
          case Some(version) =>
            if (!isStartingTag) {
              inHiddenCommentStack = inHiddenCommentStack.tail
            } else {
              val commentValue = conditionalCommentsMatcher.`match`(version, expression.toString)
              inHiddenCommentStack = (!commentValue) :: inHiddenCommentStack
            }
          case None =>
            throw new IllegalStateException("condComment call while it should be disabled")
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

        def processTag(): Unit =
          tag.getType match {

            case TagType.START | TagType.SELF_CLOSING =>

              if (tag.isRawTag && tag.nameEquals(StyleTagName)) {
                inStyle = true

              } else if (tag.nameEquals(BaseTagName)) {
                base = Option(tag.getAttributeValue(HrefAttribute)).map(_.toString)

              } else if (tag.nameEquals(LinkTagName)) {
                Option(tag.getAttributeValue(RelAttribute)) match {
                  case Some(rel) if CharSequenceUtil.equalsIgnoreCase(rel, StylesheetAttributeName) =>
                    addResource(tag, HrefAttribute, CssRawResource)
                  case Some(rel) if CharSequenceUtil.equalsIgnoreCase(rel, IconAttributeName) || CharSequenceUtil.equalsIgnoreCase(rel, ShortcutIconAttributeName) =>
                    addResource(tag, HrefAttribute, RegularRawResource)
                  case None =>
                    logger.error("Malformed HTML: <link> tag without rel attribute")
                  case _ =>
                }

              } else if (tag.nameEquals(ImgTagName) ||
                tag.nameEquals(BgsoundTagName) ||
                tag.nameEquals(EmbedTagName) ||
                tag.nameEquals(InputTagName)) {

                addResource(tag, SrcAttribute, RegularRawResource)

              } else if (tag.nameEquals(BodyTagName)) {
                addResource(tag, BackgroundAttribute, RegularRawResource)

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
                Option(tag.getAttributeValue(DataAttribute)).foreach { data =>
                  val objectResourceUrl = codeBase() match {
                    case Some(cb) => prependCodeBase(cb, data.toString)
                    case _        => data.toString
                  }
                  rawResources += RegularRawResource(objectResourceUrl)
                }

              } else {
                Option(tag.getAttributeValue(StyleAttribute)).foreach { style =>
                  val styleUrls = CssParser.extractInlineStyleImageUrls(style).map(RegularRawResource)
                  rawResources ++= styleUrls
                }
              }

            case TagType.END =>
              if (inStyle && tag.nameEquals(StyleTagName))
                inStyle = false

            case _ =>
          }

        if (!isInHiddenComment)
          processTag()
      }
    }

    try { Jodd.newLagartoParser(htmlContent, ieVersion).parse(visitor) }
    catch { case NonFatal(e) => logException(htmlContent, e) }
    HtmlResources(rawResources, base)
  }

  def getEmbeddedResources(documentURI: Uri, htmlContent: Array[Char], userAgent: Option[UserAgent]): List[EmbeddedResource] = {

    val htmlResources = parseHtml(htmlContent, userAgent)

    val rootURI = htmlResources.base.map(Uri.create(documentURI, _)).getOrElse(documentURI)

    htmlResources.rawResources
      .distinct
      .iterator
      .filterNot(res => res.rawUrl.isEmpty || res.rawUrl.charAt(0) == '#' || res.rawUrl.startsWith("data:"))
      .flatMap(_.toEmbeddedResource(rootURI))
      .toList
  }
}
