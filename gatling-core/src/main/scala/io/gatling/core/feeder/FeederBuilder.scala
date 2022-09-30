/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import com.softwaremill.quicklens._

private[gatling] trait NamedFeederBuilder extends FeederBuilder {
  def name: String
}

trait FeederBuilderBase[T] extends FeederBuilder {
  def queue: FeederBuilderBase[T]
  def random: FeederBuilderBase[T]
  def shuffle: FeederBuilderBase[T]
  def circular: FeederBuilderBase[T]
  def transform(f: PartialFunction[(String, T), Any]): FeederBuilderBase[Any]
  @deprecated("Please use transform instead.", "3.7.0")
  def convert(f: PartialFunction[(String, T), Any]): FeederBuilderBase[Any] = transform(f)
  def readRecords: Seq[Record[Any]]
  def shard: FeederBuilderBase[T]
}

trait FileBasedFeederBuilder[T] extends FeederBuilderBase[T] {
  override def queue: FileBasedFeederBuilder[T]
  override def random: FileBasedFeederBuilder[T]
  override def shuffle: FileBasedFeederBuilder[T]
  override def circular: FileBasedFeederBuilder[T]
  override def transform(f: PartialFunction[(String, T), Any]): FileBasedFeederBuilder[Any]
  override def shard: FileBasedFeederBuilder[T]
  def unzip: FileBasedFeederBuilder[T]
}

trait BatchableFeederBuilder[T] extends FileBasedFeederBuilder[T] {
  override def queue: BatchableFeederBuilder[T]
  override def random: BatchableFeederBuilder[T]
  override def shuffle: BatchableFeederBuilder[T]
  override def circular: BatchableFeederBuilder[T]
  override def transform(f: PartialFunction[(String, T), Any]): BatchableFeederBuilder[Any]
  override def shard: BatchableFeederBuilder[T]
  override def unzip: BatchableFeederBuilder[T]
  def eager: BatchableFeederBuilder[T]
  def batch: BatchableFeederBuilder[T] = batch(Batch.DefaultBufferLines)
  def batch(lines: Int): BatchableFeederBuilder[T]
}

object SourceFeederBuilder {
  def apply[T](source: FeederSource[T], configuration: GatlingConfiguration): SourceFeederBuilder[T] =
    SourceFeederBuilder(source, configuration, FeederOptions.default)
}

final case class SourceFeederBuilder[T](
    source: FeederSource[T],
    configuration: GatlingConfiguration,
    options: FeederOptions[T]
) extends BatchableFeederBuilder[T]
    with NamedFeederBuilder {

  def queue: BatchableFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Queue)
  def random: BatchableFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Random)
  def shuffle: BatchableFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Shuffle)
  def circular: BatchableFeederBuilder[T] = this.modify(_.options.strategy).setTo(FeederStrategy.Circular)

  override def transform(f: PartialFunction[(String, T), Any]): BatchableFeederBuilder[Any] = {
    val conversion: Record[T] => Record[Any] =
      _.map {
        case pair if f.isDefinedAt(pair) => pair._1 -> f(pair)
        case pair                        => pair
      }

    this.modify(_.options.conversion).setTo(Some(conversion)).asInstanceOf[BatchableFeederBuilder[Any]]
  }

  override def readRecords: Seq[Record[Any]] = apply().toVector

  override def unzip: BatchableFeederBuilder[T] = this.modify(_.options.unzip).setTo(true)

  override def eager: BatchableFeederBuilder[T] = this.modify(_.options.loadingMode).setTo(Eager)
  override def batch(bufferSize: Int): BatchableFeederBuilder[T] = this.modify(_.options.loadingMode).setTo(Batch(bufferSize))
  override def shard: BatchableFeederBuilder[T] = this.modify(_.options.shard).setTo(true)

  override def apply(): Feeder[Any] = source.feeder(options, configuration)

  override def name: String = source.name
}

private[feeder] trait FeederLoadingMode
private[feeder] case object Eager extends FeederLoadingMode
private[feeder] object Batch {
  val DefaultBufferLines: Int = 2000
}
private[feeder] final case class Batch(bufferSize: Int) extends FeederLoadingMode
private[feeder] case object Adaptive extends FeederLoadingMode

object FeederOptions {
  def default[T]: FeederOptions[T] =
    new FeederOptions[T](shard = false, unzip = false, conversion = None, strategy = FeederStrategy.Queue, loadingMode = Adaptive)
}

final case class FeederOptions[T](
    shard: Boolean,
    unzip: Boolean,
    conversion: Option[Record[T] => Record[Any]],
    strategy: FeederStrategy,
    loadingMode: FeederLoadingMode
)
