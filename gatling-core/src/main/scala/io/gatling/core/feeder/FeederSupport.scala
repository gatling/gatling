/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import scala.collection.immutable.ArraySeq

import io.gatling.commons.validation._
import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import io.gatling.core.feeder.SeparatedValuesParser._
import io.gatling.core.json.JsonParsers
import io.gatling.core.util.ResourceCache

trait FeederSupport extends ResourceCache {

  implicit def seq2FeederBuilder[T](data: IndexedSeq[Map[String, T]])(implicit configuration: GatlingConfiguration): FeederBuilderBase[T] =
    SourceFeederBuilder(InMemoryFeederSource(data), configuration)
  implicit def array2FeederBuilder[T](data: Array[Map[String, T]])(implicit configuration: GatlingConfiguration): FeederBuilderBase[T] =
    SourceFeederBuilder(InMemoryFeederSource(ArraySeq.unsafeWrapArray(data)), configuration)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def csv(fileName: String, quoteChar: Char = DefaultQuoteChar)(implicit configuration: GatlingConfiguration): BatchableFeederBuilder[String] =
    separatedValues(fileName, CommaSeparator, quoteChar)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def ssv(fileName: String, quoteChar: Char = DefaultQuoteChar)(implicit configuration: GatlingConfiguration): BatchableFeederBuilder[String] =
    separatedValues(fileName, SemicolonSeparator, quoteChar)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def tsv(fileName: String, quoteChar: Char = DefaultQuoteChar)(implicit configuration: GatlingConfiguration): BatchableFeederBuilder[String] =
    separatedValues(fileName, TabulationSeparator, quoteChar)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def separatedValues(fileName: String, separator: Char, quoteChar: Char = DefaultQuoteChar)(implicit
      configuration: GatlingConfiguration
  ): BatchableFeederBuilder[String] =
    cachedResource(GatlingFiles.resourcesDirectory(configuration), fileName) match {
      case Success(resource) => SourceFeederBuilder[String](new SeparatedValuesFeederSource(resource, separator, quoteChar), configuration)
      case Failure(message)  => throw new IllegalArgumentException(s"Could not locate feeder file: $message")
    }

  def jsonFile(fileName: String)(implicit jsonParsers: JsonParsers, configuration: GatlingConfiguration): FileBasedFeederBuilder[Any] =
    cachedResource(GatlingFiles.resourcesDirectory(configuration), fileName) match {
      case Success(resource) =>
        val data = new JsonFeederFileParser(jsonParsers).parse(resource, configuration.core.charset)
        SourceFeederBuilder(InMemoryFeederSource(data), configuration)

      case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file: $message")
    }

  def jsonUrl(url: String)(implicit jsonParsers: JsonParsers, configuration: GatlingConfiguration): FeederBuilderBase[Any] = {
    val data = new JsonFeederFileParser(jsonParsers).url(url, configuration.core.charset)
    SourceFeederBuilder(InMemoryFeederSource(data), configuration)
  }
}
