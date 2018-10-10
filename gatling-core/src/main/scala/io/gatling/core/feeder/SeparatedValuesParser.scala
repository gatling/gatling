/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.core.feeder

import java.io.{ InputStream, InputStreamReader }
import java.nio.charset.Charset

import scala.annotation.switch
import scala.collection.JavaConverters._

import io.gatling.commons.util.Io._
import io.gatling.core.util.Resource

import org.simpleflatmapper.lightningcsv.CsvParser

object SeparatedValuesParser {

  val DefaultQuoteChar: Char = '"'

  val CommaSeparator = ','
  val SemicolonSeparator = ';'
  val TabulationSeparator = '\t'

  def parse(resource: Resource, columnSeparator: Char, quoteChar: Char, charset: Charset): IndexedSeq[Record[String]] =
    withCloseable(resource.inputStream) { is =>
      stream(columnSeparator, quoteChar, charset)(is).toVector
    }

  def stream(columnSeparator: Char, quoteChar: Char, charset: Charset): InputStream => Feeder[String] = {
    val parser = CsvParser
      .separator(columnSeparator)
      .quote(quoteChar)

    is => {
      val reader = new InputStreamReader(new Utf8BomSkipInputStream(is), charset)
      val it: Iterator[Array[String]] = parser.iterator(reader).asScala
      if (it.hasNext) {
        val headers = it.next.map(_.trim)
        require(headers.nonEmpty, "CSV sources must have a non empty first line containing the headers")
        headers.foreach { header =>
          require(header.nonEmpty, "CSV headers can't be empty")
        }

        it.map { values =>
          ArrayBasedMap(headers, values)
        }
      } else {
        throw new ArrayIndexOutOfBoundsException("Feeder source is empty")
      }
    }
  }
}
