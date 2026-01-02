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

object SourceFeederBuilder {
  def apply[T](source: FeederSource[T], configuration: GatlingConfiguration): SourceFeederBuilder[T] =
    SourceFeederBuilder(source, configuration, FeederOptions.default)
}

final case class SourceFeederBuilder[T](
    source: FeederSource[T],
    configuration: GatlingConfiguration,
    options: FeederOptions[T]
) extends FileBasedFeederBuilder[T]
    with NamedFeederBuilder {
  def queue: FileBasedFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Queue)
  def random: FileBasedFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Random)
  def shuffle: FileBasedFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Shuffle)
  def circular: FileBasedFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Circular)

  override def transform(f: PartialFunction[(String, T), Any]): FileBasedFeederBuilder[Any] = {
    val conversion: Record[T] => Record[Any] =
      _.map {
        case pair if f.isDefinedAt(pair) => pair._1 -> f(pair)
        case pair                        => pair
      }

    this.modify(_.options.conversion).setTo(Some(conversion)).asInstanceOf[FileBasedFeederBuilder[Any]]
  }

  override def readRecords: Seq[Record[Any]] = apply().toVector
  override def recordsCount: Int = source.recordsCount(options, configuration)

  override def unzip: FileBasedFeederBuilder[T] = this.modify(_.options.unzip).setTo(true)

  override def shard: FileBasedFeederBuilder[T] = this.modify(_.options.shard).setTo(true)

  override def apply(): Feeder[Any] = source.feeder(options, configuration)

  override def name: String = source.name
}

object FeederOptions {
  def default[T]: FeederOptions[T] =
    new FeederOptions[T](shard = false, unzip = false, conversion = None, strategy = FeederStrategy.Queue)
}

final case class FeederOptions[T](
    shard: Boolean,
    unzip: Boolean,
    conversion: Option[Record[T] => Record[Any]],
    strategy: FeederStrategy
)
