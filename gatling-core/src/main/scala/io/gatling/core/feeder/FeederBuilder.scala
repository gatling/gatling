/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

case class SourceFeederBuilder[T](
    source:        FeederSource[T],
    configuration: GatlingConfiguration,
    options:       FeederOptions[T]     = FeederOptions[T]()
) extends FeederBuilder {

  def shard: SourceFeederBuilder[T] = this.modify(_.options.shard).setTo(true)

  def unzip: SourceFeederBuilder[T] = this.modify(_.options.unzip).setTo(true)

  def convert(f: PartialFunction[(String, T), Any]): SourceFeederBuilder[T] = {
    val conversion: Record[T] => Record[Any] =
      _.map {
        case pair if f.isDefinedAt(pair) => pair._1 -> f(pair)
        case pair                        => pair
      }

    this.modify(_.options.conversion).setTo(Some(conversion))
  }

  def queue: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(Queue)
  def random: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(Random)
  def shuffle: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(Shuffle)
  def circular: SourceFeederBuilder[T] = this.modify(_.options.strategy).setTo(Circular)

  override def apply(): Feeder[Any] = source.feeder(options, configuration)

  def readRecords: Seq[Record[Any]] = apply().toVector
}

case class FeederOptions[T](
    shard:      Boolean                          = false,
    unzip:      Boolean                          = false,
    conversion: Option[Record[T] => Record[Any]] = None,
    strategy:   FeederStrategy                   = Queue,
    batch:      Option[Int]                      = None
)

trait BatchableFeederBuilder[T] extends SourceFeederBuilder[T] {
  def batch: SourceFeederBuilder[T] = batch(2000)
  def batch(bufferSize: Int): SourceFeederBuilder[T] = copy(options = options.copy(batch = Some(bufferSize)))
}
