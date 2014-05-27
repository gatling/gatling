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

import scala.collection.breakOut
import scala.io.Source

import au.com.bytecode.opencsv.CSVParser
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Resource
import io.gatling.core.util.IO._

object SeparatedValuesParser {

  val CommaSeparator = ','
  val SemicolonSeparator = ';'
  val TabulationSeparator = '\t'

  def parse(resource: Resource, separator: Char, doubleQuote: Char): IndexedSeq[Record[String]] =
    withSource(Source.fromInputStream(resource.inputStream)(configuration.core.codec)) { source =>
      stream(source, separator, doubleQuote).toVector
    }

  def stream(source: Source, separator: Char, doubleQuote: Char): Iterator[Record[String]] = {
    val parser = new CSVParser(separator, doubleQuote)
    val rawLines = source.getLines.map(parser.parseLine)
    val headers = rawLines.next
    rawLines.map(headers.zip(_)(breakOut))
  }
}
