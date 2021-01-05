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

package io.gatling.core.util

import java.util.concurrent.ThreadLocalRandom

import scala.annotation.tailrec

import io.gatling.commons.util.Collections._

private[core] object RandomDistribution {

  def uniform[T](possibilities: List[T]): RandomDistribution[T] =
    new RandomDistribution(possibilities.map(1 -> _), possibilities.size, None)

  private val PercentWeightsNormalizingFactor = 1000000 // 100% * 1000000 < Int.MaxValue so no risk of overflowing

  def percentWeights[T](possibilities: List[(Double, T)], fallback: T): RandomDistribution[T] = {

    val sum = possibilities.sumBy(_._1)
    require(sum <= 100.000001, s"Weights sum $sum mustn't be bigger than 100%")

    val intendedTotalIs100 = math.abs(sum - 100.0) <= 0.000001

    val (_, headChain) :: tail = possibilities
    val normalizedTail: List[(Int, T)] = tail.map { case (weight, chain) =>
      (weight * PercentWeightsNormalizingFactor).toInt -> chain
    } // don't round but truncate so normalized sum doesn't because bigger than original one

    val normalizedTailSum = normalizedTail.sumBy(_._1)
    val normalizedHeadWeight =
      if (intendedTotalIs100) {
        100 * PercentWeightsNormalizingFactor - normalizedTailSum
      } else {
        (sum * PercentWeightsNormalizingFactor).round.toInt - normalizedTailSum
      }

    val normalizedPossibilities = normalizedHeadWeight -> headChain :: normalizedTail

    new RandomDistribution(normalizedPossibilities, 100 * PercentWeightsNormalizingFactor, Some(fallback))
  }
}

private[core] class RandomDistribution[T](possibilities: List[(Int, T)], max: Int, fallback: Option[T]) {

  // visible for tests
  private[util] def next(index: Int): T = {
    @tailrec
    def nextRec(index: Int, pos: List[(Int, T)]): T = pos match {
      case Nil => fallback.getOrElse(throw new UnsupportedOperationException("No fallback is defined"))
      case (weight, head) :: tail =>
        if (weight > index) {
          head
        } else {
          nextRec(index - weight, tail)
        }
    }

    nextRec(index, possibilities)
  }

  def next(): T = next(ThreadLocalRandom.current.nextInt(max))
}
