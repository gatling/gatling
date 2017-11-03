/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import java.io.InputStream
import java.util.{ Map => JMap }

import scala.collection.JavaConverters._

import io.gatling.commons.util.Io._
import io.gatling.core.util.Resource

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.csv.{ CsvSchema, CsvMapper }

object SeparatedValuesParser {

  val DefaultQuoteChar: Char = CsvSchema.DEFAULT_QUOTE_CHAR
  val DefaultEscapeChar: Char = CsvSchema.DEFAULT_ESCAPE_CHAR.toChar

  val CommaSeparator = ','
  val SemicolonSeparator = ';'
  val TabulationSeparator = '\t'

  def parse(resource: Resource, columnSeparator: Char, quoteChar: Char, escapeChar: Char): IndexedSeq[Record[String]] =
    withCloseable(resource.inputStream) { is =>
      stream(columnSeparator, quoteChar, escapeChar)(is).toVector
    }

  def stream(columnSeparator: Char, quoteChar: Char, escapeChar: Char): InputStream => Feeder[String] = {
    val mapper = new CsvMapper().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    val schema = CsvSchema.emptySchema.withHeader.withColumnSeparator(columnSeparator).withQuoteChar(quoteChar).withEscapeChar(escapeChar)
    val reader = mapper.readerFor(classOf[JMap[_, _]]).`with`(schema)

    is => {
      val it: Iterator[JMap[String, String]] = reader.readValues(is).asScala
      it.map(_.asScala.toMap)
    }
  }
}
