/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.feeder

import java.io.InputStream

import scala.collection.breakOut
import scala.collection.mutable
import scala.xml.Node

import io.gatling.core.config.Resource
import io.gatling.core.feeder.Record
import io.gatling.core.util.IOHelper._

/**
 * Parser for files in [[http://www.sitemaps.org/protocol.html sitemap]] format.
 *
 * @author Ivan Mushketyk
 */
object SitemapParser {

  val LOCATION_TAG = "loc"

  /**
   * Parse file in sitemap format. Returns a Record for each location described
   * in a sitemap file.
   *
   * @param resource resource to parse
   * @return a record for each url described in a sitemap file
   */
  def parse(resource: Resource): IndexedSeq[Record[String]] = {
    withCloseable(resource.inputStream) { stream: InputStream =>
      parse(stream)
    }
  }

  /**
   * Parse a file in sitemap format. Returns a Record for each location described
   * in a sitemap file.
   *
   * @param inputStream stream for the file to parse
   * @return a record for each url described in a sitemap file
   */
  def parse(inputStream: InputStream): IndexedSeq[Record[String]] = {
    val records = mutable.ArrayBuffer[Record[String]]()

    val urlsetElem = scala.xml.XML.load(inputStream)
    (urlsetElem \ "url").foreach(url => {

      val record: Map[String, String] = url.child.collect {
        case node: xml.Elem =>
          val nodeName = name(node)
          val textValue = text(node)
          nodeName -> textValue
      }(breakOut)

      if (!record.contains(LOCATION_TAG) || record(LOCATION_TAG).isEmpty)
        throw new SitemapFormatException("No 'loc' child in 'url' element")

      records += record
    })

    records
  }

  private def name(node: Node): String = {
    val sb = new StringBuilder
    node.nameToString(sb)
    sb.toString
  }

  private def text(node: Node): String = {
    if (!node.child.isEmpty)
      node.child.head.toString
    else
      ""
  }
}

class SitemapFormatException(msg: String) extends Exception
