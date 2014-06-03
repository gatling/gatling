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

import scala.reflect.io.File

import io.gatling.core.config.Resource
import SeparatedValuesParser.{ CommaSeparator, SemicolonSeparator, TabulationSeparator }
import io.gatling.core.validation.{ Failure, Success, Validation }

trait FeederSupport {

  type Feeder[T] = io.gatling.core.feeder.Feeder[T]

  implicit def seq2FeederBuilder[T](data: IndexedSeq[Map[String, T]]): RecordSeqFeederBuilder[T] = RecordSeqFeederBuilder(data)
  implicit def array2FeederBuilder[T](data: Array[Map[String, T]]): RecordSeqFeederBuilder[T] = RecordSeqFeederBuilder(data)
  implicit def feeder2FeederBuilder[T](feeder: Feeder[T]): FeederBuilder[T] = FeederWrapper(feeder)

  def csv(file: File, rawSplit: Boolean): RecordSeqFeederBuilder[String] = csv(file.path, rawSplit)

  def csv(fileName: String, rawSplit: Boolean = false): RecordSeqFeederBuilder[String] = separatedValues(fileName, CommaSeparator, rawSplit = rawSplit)

  def ssv(file: File, rawSplit: Boolean): RecordSeqFeederBuilder[String] = ssv(file.path, rawSplit)

  def ssv(fileName: String, rawSplit: Boolean = false): RecordSeqFeederBuilder[String] = separatedValues(fileName, SemicolonSeparator, rawSplit = rawSplit)

  def tsv(file: File, rawSplit: Boolean): RecordSeqFeederBuilder[String] = tsv(file.path, rawSplit)

  def tsv(fileName: String, rawSplit: Boolean = false): RecordSeqFeederBuilder[String] = separatedValues(fileName, TabulationSeparator, rawSplit = rawSplit)

  def separatedValues(fileName: String, separator: Char, quoteChar: Char = '"', rawSplit: Boolean = false): RecordSeqFeederBuilder[String] =
    separatedValues(Resource.feeder(fileName), separator, quoteChar, rawSplit)

  def separatedValues(resource: Validation[Resource], separator: Char, quoteChar: Char, rawSplit: Boolean): RecordSeqFeederBuilder[String] =
    resource match {
      case Success(res)     => RecordSeqFeederBuilder(SeparatedValuesParser.parse(res, separator, quoteChar, rawSplit))
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file; $message")
    }
}
