/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.feeder.SeparatedValuesParser._
import io.gatling.core.json.JsonParsers
import io.gatling.core.util.Resource

trait FeederSupport {

  type Feeder[T] = io.gatling.core.feeder.Feeder[T]
  type FeederBuilder[T] = io.gatling.core.feeder.FeederBuilder[T]

  implicit def seq2FeederBuilder[T](data: IndexedSeq[Map[String, T]]): RecordSeqFeederBuilder[T] = RecordSeqFeederBuilder(data)
  implicit def array2FeederBuilder[T](data: Array[Map[String, T]]): RecordSeqFeederBuilder[T] = RecordSeqFeederBuilder(data)
  implicit def feeder2FeederBuilder[T](feeder: Feeder[T]): FeederBuilder[T] = FeederWrapper(feeder)

  def csv(fileName: String, quoteChar: Char = '"', escapeChar: Char = 0)(implicit configuration: GatlingConfiguration): RecordSeqFeederBuilder[String] =
    separatedValues(fileName, CommaSeparator, quoteChar, escapeChar)
  def ssv(fileName: String, quoteChar: Char = '"', escapeChar: Char = 0)(implicit configuration: GatlingConfiguration): RecordSeqFeederBuilder[String] =
    separatedValues(fileName, SemicolonSeparator, quoteChar, escapeChar)
  def tsv(fileName: String, quoteChar: Char = '"', escapeChar: Char = 0)(implicit configuration: GatlingConfiguration): RecordSeqFeederBuilder[String] =
    separatedValues(fileName, TabulationSeparator, quoteChar, escapeChar)

  def separatedValues(fileName: String, separator: Char, quoteChar: Char = '"', escapeChar: Char = 0)(implicit configuration: GatlingConfiguration): RecordSeqFeederBuilder[String] =
    separatedValues(Resource.feeder(fileName), separator, quoteChar, escapeChar)

  def separatedValues(resource: Validation[Resource], separator: Char, quoteChar: Char, escapeChar: Char)(implicit configuration: GatlingConfiguration): RecordSeqFeederBuilder[String] =
    feederBuilder(resource)(SeparatedValuesParser.parse(_, separator, quoteChar, escapeChar))

  def jsonFile(fileName: String)(implicit configuration: GatlingConfiguration, jsonParsers: JsonParsers): RecordSeqFeederBuilder[Any] = jsonFile(Resource.feeder(fileName))
  def jsonFile(resource: Validation[Resource])(implicit jsonParsers: JsonParsers): RecordSeqFeederBuilder[Any] =
    feederBuilder(resource)(new JsonFeederFileParser().parse)

  def feederBuilder[T](resource: Validation[Resource])(recordParser: Resource => IndexedSeq[Record[T]]): RecordSeqFeederBuilder[T] =
    resource match {
      case Success(res)     => RecordSeqFeederBuilder(recordParser(res))
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file: $message")
    }

  def jsonUrl(url: String)(implicit jsonParsers: JsonParsers) = RecordSeqFeederBuilder(new JsonFeederFileParser().url(url))
}
