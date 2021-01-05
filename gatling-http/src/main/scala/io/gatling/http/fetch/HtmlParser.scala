/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import scala.collection.mutable
import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.core.check.css.Lagarto
import io.gatling.http.client.uri.Uri
import io.gatling.http.util.HttpHelper

import com.typesafe.scalalogging.StrictLogging
import jodd.lagarto.{ EmptyTagVisitor, Tag, TagType }
import jodd.util.CharSequenceUtil

private[fetch] sealed abstract class RawResource {
  def rawUrl: String
  def uri(rootURI: Uri): Option[Uri] = HttpHelper.resolveFromUriSilently(rootURI, rawUrl)
  def toEmbeddedResource(rootURI: Uri): Option[ConcurrentResource]
}
private[fetch] final case class CssRawResource(rawUrl: String) extends RawResource {
  def toEmbeddedResource(rootURI: Uri): Option[ConcurrentResource] = uri(rootURI).map(CssResource)
}
private[fetch] final case class RegularRawResource(rawUrl: String) extends RawResource {
  def toEmbeddedResource(rootURI: Uri): Option[ConcurrentResource] = uri(rootURI).map(BasicResource)
}

private[fetch] final case class HtmlResources(rawResources: Seq[RawResource], base: Option[String])

private[gatling] object HtmlParser extends StrictLogging {
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
      logger.debug(
        s"""HTML parser crashed, there's a chance your page wasn't proper HTML:
>>>>>>>>>>>>>>>>>>>>>>>
${new String(htmlContent)}
<<<<<<<<<<<<<<<<<<<<<<<""",
        e
      )
    else
      logger.error(
        s"HTML parser crashed: ${e.rootMessage}, there's a chance your page wasn't proper HTML, enable debug on 'io.gatling.http.fetch' logger to get the HTML content",
        e
      )
}

class HtmlParser extends StrictLogging {

  import HtmlParser._

  var inStyle = false

  private def parseHtml(htmlContent: Array[Char]): HtmlResources = {

    var base: Option[String] = None
    val rawResources = mutable.ArrayBuffer.empty[RawResource]

    val visitor: EmptyTagVisitor = new EmptyTagVisitor {

      def addResource(tag: Tag, attributeName: String, factory: String => RawResource): Unit =
        Option(tag.getAttributeValue(attributeName)).foreach { url =>
          rawResources += factory(url.toString)
        }

      override def script(tag: Tag, body: CharSequence): Unit =
        addResource(tag, SrcAttribute, RegularRawResource)

      override def text(text: CharSequence): Unit =
        if (inStyle)
          rawResources ++= CssParser.extractStyleImportsUrls(text).map(CssRawResource)

      override def tag(tag: Tag): Unit = {

        def codeBase(): Option[CharSequence] = Option(tag.getAttributeValue(CodeBaseAttribute))

        def prependCodeBase(codeBase: CharSequence, url: String): String =
          if (url.startsWith("http")) {
            url
          } else if (codeBase.charAt(codeBase.length()) != '/') {
            s"$codeBase/$url"
          } else {
            s"$codeBase$url"
          }

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
                  case Some(rel)
                      if CharSequenceUtil.equalsIgnoreCase(rel, IconAttributeName) || CharSequenceUtil.equalsIgnoreCase(rel, ShortcutIconAttributeName) =>
                    addResource(tag, HrefAttribute, RegularRawResource)
                  case _ =>
                }

              } else if (
                tag.nameEquals(ImgTagName) ||
                tag.nameEquals(BgsoundTagName) ||
                tag.nameEquals(EmbedTagName) ||
                tag.nameEquals(InputTagName)
              ) {

                addResource(tag, SrcAttribute, RegularRawResource)

              } else if (tag.nameEquals(BodyTagName)) {
                addResource(tag, BackgroundAttribute, RegularRawResource)

              } else if (tag.nameEquals(AppletTagName)) {
                val code = tag.getAttributeValue(CodeAttribute).toString
                val archives = Option(tag.getAttributeValue(ArchiveAttribute)).map(_.toString.split(",").view.map(_.trim).to(Seq))

                val appletResources = archives.getOrElse(code :: Nil).iterator
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

        processTag()
      }
    }

    try {
      Lagarto.newLagartoParser(htmlContent).parse(visitor)
    } catch { case NonFatal(e) => logException(htmlContent, e) }
    HtmlResources(rawResources.toSeq, base)
  }

  def getEmbeddedResources(documentURI: Uri, htmlContent: Array[Char]): List[ConcurrentResource] = {

    val htmlResources = parseHtml(htmlContent)

    val rootURI = htmlResources.base.map(Uri.create(documentURI, _)).getOrElse(documentURI)

    htmlResources.rawResources.view.distinct
      .filterNot(res => res.rawUrl.isEmpty || res.rawUrl.charAt(0) == '#' || res.rawUrl.startsWith("data:"))
      .flatMap(_.toEmbeddedResource(rootURI).toList)
      .to(List)
  }
}
