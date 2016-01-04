/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.commons.util.RoundRobin
import io.gatling.core.structure.ScenarioContext

sealed trait FeederStrategy {
  def feeder[T](records: IndexedSeq[Record[T]], ctx: ScenarioContext): Feeder[T]
}

case object Queue extends FeederStrategy {
  def feeder[T](records: IndexedSeq[Record[T]], ctx: ScenarioContext): Feeder[T] =
    records.iterator
}

case object Random extends FeederStrategy {
  def feeder[T](records: IndexedSeq[Record[T]], ctx: ScenarioContext): Feeder[T] =
    new Feeder[T] {
      def hasNext = records.length != 0
      def next = records(ThreadLocalRandom.current.nextInt(records.length))
    }
}

case object Shuffle extends FeederStrategy {
  def feeder[T](records: IndexedSeq[Record[T]], ctx: ScenarioContext): Feeder[T] =
    scala.util.Random.shuffle(records).iterator
}

case object Circular extends FeederStrategy {
  def feeder[T](records: IndexedSeq[Record[T]], ctx: ScenarioContext): Feeder[T] =
    RoundRobin(records)
}
