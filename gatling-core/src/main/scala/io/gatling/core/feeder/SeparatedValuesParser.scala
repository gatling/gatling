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
package io.gatling.core.feeder

import java.io.InputStream
import java.util.{ Map => JMap }

import com.fasterxml.jackson.databind.{ ObjectReader, MappingIterator }

import scala.collection.JavaConversions._

import com.fasterxml.jackson.dataformat.csv.{ CsvSchema, CsvMapper }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.Resource
import io.gatling.core.util.Io._

object SeparatedValuesParser {

  val CommaSeparator = ','
  val SemicolonSeparator = ';'
  val TabulationSeparator = '\t'

  def parse(resource: Resource, columnSeparator: Char, quoteChar: Char, rawSplit: Boolean)(implicit configuration: GatlingConfiguration): IndexedSeq[Record[String]] =
    withCloseable(resource.inputStream) { source =>
      stream(source, columnSeparator, quoteChar, rawSplit).toVector
    }

  def stream(is: InputStream, columnSeparator: Char, quoteChar: Char, rawSplit: Boolean): Iterator[Record[String]] = {

    val mapper = new CsvMapper
    val schema = CsvSchema.emptySchema.withHeader.withColumnSeparator(columnSeparator).withQuoteChar(quoteChar).withEscapeChar('\\')

    val reader: ObjectReader = mapper.reader(classOf[JMap[_, _]])

    val it: MappingIterator[JMap[String, String]] = reader
      .`with`(schema)
      .readValues(is)

    it.map(_.toMap)
  }
}
