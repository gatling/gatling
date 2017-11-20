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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.Resource

sealed trait FeederSource[T] {
  def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any]
}

case class InMemoryFeederSource[T](records: IndexedSeq[Record[T]]) extends FeederSource[T] {

  require(records.nonEmpty, "Feeder must not be empty")

  override def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any] = {

    val rawRecords: IndexedSeq[Record[T]] =
      configuration.resolve(
        // [fl]
        //
        //
        //
        //
        //
        //
        // [fl]
        records
      )

    InMemoryFeeder(rawRecords, options.conversion, options.strategy)
  }
}

class SeparatedValuesFeederSource(resource: Resource, separator: Char, quoteChar: Char) extends FeederSource[String] {
  override def feeder(options: FeederOptions[String], configuration: GatlingConfiguration): Feeder[Any] = {
    val charset = configuration.core.charset
    configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      // [fl]
      if (options.batched) {
        BatchedSeparatedValuesFeeder(resource.file, separator, quoteChar, options.conversion, options.strategy, options.batchBufferSize, charset)
      } else {
        val records = SeparatedValuesParser.parse(resource, separator, quoteChar, charset)
        InMemoryFeeder(records, options.conversion, options.strategy)
      }
    )
  }
}
