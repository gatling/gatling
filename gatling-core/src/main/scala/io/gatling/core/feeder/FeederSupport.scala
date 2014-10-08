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

import io.gatling.core.config.Resource
import SeparatedValuesParser.{ CommaSeparator, SemicolonSeparator, TabulationSeparator }
import io.gatling.core.validation.{ Failure, Success, Validation }

trait FeederSupport {

  type Feeder[T] = io.gatling.core.feeder.Feeder[T]

  implicit def seq2FeederBuilder[T](data: IndexedSeq[Map[String, T]]): RecordSeqFeederBuilder[T] = RecordSeqFeederBuilder(data)
  implicit def array2FeederBuilder[T](data: Array[Map[String, T]]): RecordSeqFeederBuilder[T] = RecordSeqFeederBuilder(data)
  implicit def feeder2FeederBuilder[T](feeder: Feeder[T]): FeederBuilder[T] = FeederWrapper(feeder)

  def csv(fileName: String, rawSplit: Boolean = false): RecordSeqFeederBuilder[String] = separatedValues(fileName, CommaSeparator, rawSplit = rawSplit)
  def ssv(fileName: String, rawSplit: Boolean = false): RecordSeqFeederBuilder[String] = separatedValues(fileName, SemicolonSeparator, rawSplit = rawSplit)
  def tsv(fileName: String, rawSplit: Boolean = false): RecordSeqFeederBuilder[String] = separatedValues(fileName, TabulationSeparator, rawSplit = rawSplit)

  def separatedValues(fileName: String, separator: Char, quoteChar: Char = '"', rawSplit: Boolean = false): RecordSeqFeederBuilder[String] =
    separatedValues(Resource.feeder(fileName), separator, quoteChar, rawSplit)

  def separatedValues(resource: Validation[Resource], separator: Char, quoteChar: Char, rawSplit: Boolean): RecordSeqFeederBuilder[String] =
    feederBuilder(resource)(SeparatedValuesParser.parse(_, separator, quoteChar, rawSplit))

  def jsonFile(fileName: String): RecordSeqFeederBuilder[Any] = jsonFile(Resource.feeder(fileName))
  def jsonFile(resource: Validation[Resource]): RecordSeqFeederBuilder[Any] =
    feederBuilder(resource)(JsonFeederFileParser.parse)

  def feederBuilder[T](resource: Validation[Resource])(recordParser: Resource => IndexedSeq[Record[T]]): RecordSeqFeederBuilder[T] =
    resource match {
      case Success(res)     => RecordSeqFeederBuilder(recordParser(res))
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file; $message")
    }

  def jsonUrl(url: String) = RecordSeqFeederBuilder(JsonFeederFileParser.url(url))
}
