/**
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

import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }

import io.gatling.core.structure.ScenarioContext

trait FeederBuilder[T] {
  def build(ctx: ScenarioContext): Feeder[T]
}

case class FeederWrapper[T](feeder: Feeder[T]) extends FeederBuilder[T] {
  def build(ctx: ScenarioContext) = feeder
}

/**
  * A [[BatchedFeederBuilder]] builds a [[Feeder]] which delegates to one or more
  * underlaying [[Feeder]]s. Each of the underlaying [[Feeder]]s wraps a single batch
  * of records.
  *
  * @param records an Iterator over all records available
  * @param batchSize defines the record size per underlaying Feeder
  * @param minUsedItemsPerBatch defines the minimum number of times you want to consume
  *                             from the current batch before moving on to the next batch
  * @param strategy the FeederStrategy for each underlaying Feeder
  * @tparam T the record type
  */
case class BatchedFeederBuilder[T](
    records:              Iterator[Record[T]],
    batchSize:            Long,
    minUsedItemsPerBatch: Long,
    strategy:             FeederStrategy      = Queue
) extends FeederBuilder[T] {

  override def build(ctx: ScenarioContext): Feeder[T] = {
      def nextBatch(): Feeder[T] = {
        val batchRecords = (1L to batchSize).toIterator.zip(records).map {
          case (_, record) =>
            record
        }.toIndexedSeq
        strategy.feeder(batchRecords, ctx)
      }

    var currentBatch: Feeder[T] = nextBatch()

    val itemsUsed = new AtomicLong(-1)

    new Feeder[T] {
      def hasNext: Boolean = {
        if (itemsUsed.incrementAndGet() == minUsedItemsPerBatch) {
          currentBatch = nextBatch()
          itemsUsed.set(-1)
        }
        currentBatch.hasNext
      }
      def next: Record[T] = currentBatch.next()
    }
  }
}

case class RecordSeqFeederBuilder[T](
    records: IndexedSeq[Record[T]],
    // [fl]
    //
    // [fl]
    strategy: FeederStrategy = Queue
) extends FeederBuilder[T] {

  def convert(conversion: PartialFunction[(String, T), Any]): RecordSeqFeederBuilder[Any] = {
    val useValueAsIs: PartialFunction[(String, T), Any] = { case (_, value) => value }
    val fullConversion = conversion orElse useValueAsIs

    copy[Any](records = records.map(_.map { case (key, value) => key -> fullConversion(key -> value) }))
  }

  def build(ctx: ScenarioContext): Feeder[T] =
    ctx.coreComponents.configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      //
      //
      // [fl]
      strategy.feeder(records, ctx)
    )

  def queue = copy(strategy = Queue)
  def random = copy(strategy = Random)
  def shuffle = copy(strategy = Shuffle)
  def circular = copy(strategy = Circular)

  // [fl]
  //
  // [fl]
}
