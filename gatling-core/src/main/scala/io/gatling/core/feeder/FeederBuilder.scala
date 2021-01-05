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

import io.gatling.core.config.GatlingConfiguration

import com.softwaremill.quicklens._

trait FeederBuilderBase[T] extends FeederBuilder {
  type F <: FeederBuilderBase[T]
  def queue: F
  def random: F
  def shuffle: F
  def circular: F
  def convert(f: PartialFunction[(String, T), Any]): F
  def readRecords: Seq[Record[Any]]
  def shard: F
}

trait FileBasedFeederBuilder[T] extends FeederBuilderBase[T] {
  def unzip: F
}

trait BatchableFeederBuilder[T] extends FileBasedFeederBuilder[T] {
  override type F <: BatchableFeederBuilder[T]
  def eager: BatchableFeederBuilder[T]
  def batch: BatchableFeederBuilder[T] = batch(Batch.DefaultBufferSize)
  def batch(bufferSize: Int): BatchableFeederBuilder[T]
}

object SourceFeederBuilder {
  def apply[T](source: FeederSource[T], configuration: GatlingConfiguration): SourceFeederBuilder[T] =
    new SourceFeederBuilder(source, configuration, FeederOptions.default)
}

final case class SourceFeederBuilder[T](
    source: FeederSource[T],
    configuration: GatlingConfiguration,
    options: FeederOptions[T]
) extends BatchableFeederBuilder[T] {

  override type F = BatchableFeederBuilder[T]

  def queue: F = this.modify(_.options.strategy).setTo(Queue)
  def random: F = this.modify(_.options.strategy).setTo(Random)
  def shuffle: F = this.modify(_.options.strategy).setTo(Shuffle)
  def circular: F = this.modify(_.options.strategy).setTo(Circular)

  override def convert(f: PartialFunction[(String, T), Any]): F = {
    val conversion: Record[T] => Record[Any] =
      _.map {
        case pair if f.isDefinedAt(pair) => pair._1 -> f(pair)
        case pair                        => pair
      }

    this.modify(_.options.conversion).setTo(Some(conversion))
  }

  override def readRecords: Seq[Record[Any]] = apply().toVector

  override def unzip: F = this.modify(_.options.unzip).setTo(true)

  override def eager: F = this.modify(_.options.loadingMode).setTo(Eager)
  override def batch(bufferSize: Int): F = this.modify(_.options.loadingMode).setTo(Batch(bufferSize))
  override def shard: F = this.modify(_.options.shard).setTo(true)

  override def apply(): Feeder[Any] = source.feeder(options, configuration)
}

private[feeder] trait FeederLoadingMode
private[feeder] case object Eager extends FeederLoadingMode
private[feeder] object Batch {
  val DefaultBufferSize: Int = 2000
}
private[feeder] final case class Batch(bufferSize: Int) extends FeederLoadingMode
private[feeder] case object Adaptive extends FeederLoadingMode

object FeederOptions {
  def default[T]: FeederOptions[T] = new FeederOptions[T](shard = false, unzip = false, conversion = None, strategy = Queue, loadingMode = Adaptive)
}

final case class FeederOptions[T](
    shard: Boolean,
    unzip: Boolean,
    conversion: Option[Record[T] => Record[Any]],
    strategy: FeederStrategy,
    loadingMode: FeederLoadingMode
)
