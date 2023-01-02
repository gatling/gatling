/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import java.util.concurrent.ThreadLocalRandom

import io.gatling.commons.util.CircularIterator

private[gatling] object InMemoryFeeder {
  def apply[T](records: IndexedSeq[Record[T]], conversion: Option[Record[T] => Record[Any]], strategy: FeederStrategy): Feeder[Any] = {
    val convertedRecords = conversion match {
      case Some(f) => records.map(f)
      case _       => records
    }

    strategy match {
      case FeederStrategy.Queue    => convertedRecords.iterator
      case FeederStrategy.Random   => Iterator.continually(convertedRecords(ThreadLocalRandom.current.nextInt(records.length)))
      case FeederStrategy.Shuffle  => scala.util.Random.shuffle(convertedRecords).iterator
      case FeederStrategy.Circular => CircularIterator(convertedRecords, threadSafe = false)
    }
  }
}
