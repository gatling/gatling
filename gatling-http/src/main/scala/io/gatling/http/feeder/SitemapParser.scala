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

package io.gatling.http.feeder

import java.io.InputStream
import java.nio.charset.Charset

import scala.jdk.CollectionConverters._
import scala.util.Using

import io.gatling.core.check.xpath.XmlParsers
import io.gatling.core.feeder.Record
import io.gatling.core.util.Resource

import net.sf.saxon.s9api.XdmNodeKind

/**
 * Parser for files in [[http://www.sitemaps.org/protocol.html sitemap]] format.
 */
object SitemapParser {

  val LocationTag = "loc"

  /**
   * Parse file in sitemap format. Returns a Record for each location described
   * in a sitemap file.
   *
   * @param resource resource to parse
   * @return a record for each url described in a sitemap file
   */
  def parse(resource: Resource, charset: Charset): IndexedSeq[Record[String]] =
    Using.resource(resource.inputStream) { stream: InputStream =>
      parse(stream, charset)
    }

  /**
   * Parse a file in sitemap format. Returns a Record for each location described
   * in a sitemap file.
   *
   * @param inputStream stream for the file to parse
   * @return a record for each url described in a sitemap file
   */
  private[feeder] def parse(inputStream: InputStream, charset: Charset): IndexedSeq[Record[String]] = {

    val root = XmlParsers.parse(inputStream, charset)

    (for {
      urlset <- root.children("urlset").asScala if urlset.getNodeKind == XdmNodeKind.ELEMENT
      url <- urlset.children("url").asScala if urlset.getNodeKind == XdmNodeKind.ELEMENT
    } yield {
      val urlChildren = url.children.asScala.toVector

      val record = urlChildren.collect {
        case child if child.getNodeKind == XdmNodeKind.ELEMENT =>
          child.getNodeName.getLocalName -> child.getStringValue
      }.toMap
      if (!record.contains(LocationTag) || record(LocationTag).isEmpty) {
        throw new SitemapFormatException("No 'loc' child in 'url' element")
      }
      record
    }).toVector
  }
}

class SitemapFormatException(msg: String) extends Exception
