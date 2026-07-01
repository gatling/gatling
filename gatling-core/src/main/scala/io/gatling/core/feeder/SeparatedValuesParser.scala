/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.nio.channels.{ Channels, ReadableByteChannel }
import java.nio.charset.Charset

import scala.jdk.CollectionConverters._

import org.simpleflatmapper.lightningcsv.CsvParser

private[gatling] object SeparatedValuesParser {
  val DefaultQuoteChar: Char = '"'
  val CommaSeparator: Char = ','
  val SemicolonSeparator: Char = ';'
  val TabulationSeparator: Char = '\t'

  def feederFactory(columnSeparator: Char, quoteChar: Char, charset: Charset): ReadableByteChannel => Feeder[String] = {
    val parser = CsvParser
      .separator(columnSeparator)
      .quote(quoteChar)

    channel => {
      val reader = Channels.newReader(new Utf8BomSkipReadableByteChannel(channel), charset.newDecoder, -1)
      val it = parser.iterator(reader)

      require(it.hasNext, "CSV feeder files mustn't be empty")
      val headers = it.next().map(_.trim)
      require(headers.nonEmpty, "CSV feeder files must have a first line with column headers")
      require(headers.forall(_.nonEmpty), s"CSV feeder headers mustn't be empty Strings, found ${headers.mkString("(", ", ", ")")}")

      it.asScala.collect { case row if !(row.length == 1 && row(0).isEmpty) => ArrayBasedMap(headers, row) }
    }
  }
}
