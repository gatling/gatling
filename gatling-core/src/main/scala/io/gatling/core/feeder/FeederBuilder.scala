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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.internal.quicklens._

private[gatling] trait NamedFeederBuilder extends FeederBuilder {
  def name: String
}

sealed trait FeederBuilderBase[T] extends FeederBuilder {
  def queue: FeederBuilderBase[T]
  def random: FeederBuilderBase[T]
  def shuffle: FeederBuilderBase[T]
  def circular: FeederBuilderBase[T]
  def transform(f: PartialFunction[(String, T), Any]): FeederBuilderBase[Any]
  def readRecords: Seq[Record[Any]]
  def recordsCount: Int
  def shard: FeederBuilderBase[T]
}

sealed trait FileBasedFeederBuilder[T] extends FeederBuilderBase[T] {
  override def queue: FileBasedFeederBuilder[T]
  override def random: FileBasedFeederBuilder[T]
  override def shuffle: FileBasedFeederBuilder[T]
  override def circular: FileBasedFeederBuilder[T]
  override def transform(f: PartialFunction[(String, T), Any]): FileBasedFeederBuilder[Any]
  override def shard: FileBasedFeederBuilder[T]
  def unzip: FileBasedFeederBuilder[T]
}

sealed trait SeparatedValuesFeederBuilder[T] extends FileBasedFeederBuilder[T] {
  override def queue: SeparatedValuesFeederBuilder[T]
  override def random: SeparatedValuesFeederBuilder[T]
  override def shuffle: SeparatedValuesFeederBuilder[T]
  override def circular: SeparatedValuesFeederBuilder[T]
  override def transform(f: PartialFunction[(String, T), Any]): SeparatedValuesFeederBuilder[Any]
  override def shard: SeparatedValuesFeederBuilder[T]
  override def unzip: SeparatedValuesFeederBuilder[T]

  /**
   * Provide the column names of a file that doesn't have a header line. The first line of the file is then a record like all the other ones.
   */
  def headers(firstHeader: String, otherHeaders: String*): SeparatedValuesFeederBuilder[T]
}

object SourceFeederBuilder {
  def apply[T](source: FeederSource[T], configuration: GatlingConfiguration): SourceFeederBuilder[T] =
    SourceFeederBuilder(source, configuration, FeederOptions.default)
}

final case class SourceFeederBuilder[T](
    source: FeederSource[T],
    configuration: GatlingConfiguration,
    options: FeederOptions[T]
) extends SeparatedValuesFeederBuilder[T]
    with NamedFeederBuilder {
  def queue: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Queue)
  def random: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Random)
  def shuffle: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Shuffle)
  def circular: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Circular)

  override def transform(f: PartialFunction[(String, T), Any]): SourceFeederBuilder[Any] = {
    val conversion: Record[T] => Record[Any] =
      _.map {
        case pair if f.isDefinedAt(pair) => pair._1 -> f(pair)
        case pair                        => pair
      }

    this.modify(_.options.conversion).setTo(Some(conversion)).asInstanceOf[SourceFeederBuilder[Any]]
  }

  override def readRecords: Seq[Record[Any]] = apply().toVector
  override def recordsCount: Int = source.recordsCount(options, configuration)

  override def unzip: SourceFeederBuilder[T] = this.modify(_.options.unzip).setTo(true)

  override def shard: SourceFeederBuilder[T] = this.modify(_.options.shard).setTo(true)

  override def headers(firstHeader: String, otherHeaders: String*): SourceFeederBuilder[T] = {
    val headers = firstHeader +: otherHeaders

    require(
      headers.forall(header => header != null && header.nonEmpty),
      s"Feeder headers mustn't be empty Strings, found ${headers.mkString("(", ", ", ")")}"
    )
    require(headers.distinct.lengthIs == headers.length, s"Feeder headers mustn't contain duplicates, found ${headers.mkString("(", ", ", ")")}")

    this.modify(_.options.headers).setTo(Some(headers))
  }

  override def apply(): Feeder[Any] = source.feeder(options, configuration)

  override def name: String = source.name
}

object FeederOptions {
  def default[T]: FeederOptions[T] =
    new FeederOptions[T](shard = false, unzip = false, conversion = None, strategy = FeederStrategy.Queue, headers = None)
}

final case class FeederOptions[T](
    shard: Boolean,
    unzip: Boolean,
    conversion: Option[Record[T] => Record[Any]],
    strategy: FeederStrategy,
    headers: Option[Seq[String]]
)
