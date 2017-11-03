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

import com.softwaremill.quicklens._

case class SourceFeederBuilder[T](
    source:        FeederSource[T],
    configuration: GatlingConfiguration,
    options:       FeederOptions[T]     = FeederOptions[T]()
) extends FeederBuilder {
  // [fl]
  //
  // [fl]

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
}

case class FeederOptions[T](
    // [fl]
    shardingEnabled: Boolean = false,
    // [fl]
    conversion:      Option[Record[T] => Record[Any]] = None,
    strategy:        FeederStrategy                   = Queue,
    batched:         Boolean                          = false,
    batchBufferSize: Int                              = 2000
)

trait BatchableFeederBuilder[T] extends SourceFeederBuilder[T] {
  def batched: SourceFeederBuilder[T] = copy(options = options.copy(batched = true))
  def batched(bufferSize: Int): SourceFeederBuilder[T] = copy(options = options.copy(batched = true, batchBufferSize = bufferSize))
}
