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

package io.gatling.core.pause

import java.util.concurrent.ThreadLocalRandom

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.session._

sealed abstract class PauseType {
  def generator(duration: FiniteDuration): Expression[Long] = generator(duration.expressionSuccess)
  def generator(duration: Expression[FiniteDuration]): Expression[Long]
}

object Disabled extends PauseType {
  override def generator(duration: Expression[FiniteDuration]): Expression[Long] = throw new UnsupportedOperationException
}

object Constant extends PauseType {
  override def generator(duration: Expression[FiniteDuration]): Expression[Long] = duration.map(_.toMillis)
}

object Exponential extends PauseType {

  private def nextValue = {
    val rnd = ThreadLocalRandom.current
    var u = 0d
    do {
      u = rnd.nextDouble
    } while (u == 0d)
    -Math.log(u)
  }

  override def generator(duration: Expression[FiniteDuration]): Expression[Long] =
    duration.map(duration => (nextValue * duration.toMillis).round)
}

final class NormalWithPercentageDuration(stdDev: Double) extends PauseType {

  private val stdDevPercent = stdDev / 100.0

  override def generator(duration: Expression[FiniteDuration]): Expression[Long] =
    duration.map(d => math.max(0L, ((1 + ThreadLocalRandom.current.nextGaussian * stdDevPercent) * d.toMillis).toLong))
}

final class NormalWithStdDevDuration(stdDev: FiniteDuration) extends PauseType {
  override def generator(duration: Expression[FiniteDuration]): Expression[Long] =
    duration.map(d => math.max(0L, (ThreadLocalRandom.current.nextGaussian * stdDev.toMillis + d.toMillis).toLong))
}

final class Custom(custom: Expression[Long]) extends PauseType {
  override def generator(duration: Expression[FiniteDuration]): Expression[Long] = custom
}

final class UniformPercentage(plusOrMinus: Double) extends PauseType {

  private val plusOrMinusPercent = plusOrMinus / 100.0

  override def generator(duration: Expression[FiniteDuration]): Expression[Long] =
    duration.map { d =>
      val mean = d.toMillis
      val halfWidth = (mean * plusOrMinusPercent).round
      val least = math.max(0L, mean - halfWidth)
      val bound = mean + halfWidth + 1
      ThreadLocalRandom.current.nextLong(least, bound)
    }
}

final class UniformDuration(plusOrMinus: FiniteDuration) extends PauseType {

  private val halfWidth = plusOrMinus.toMillis

  override def generator(duration: Expression[FiniteDuration]): Expression[Long] =
    duration.map { duration =>
      val mean = duration.toMillis
      val least = math.max(0L, mean - halfWidth)
      val bound = mean + halfWidth + 1
      ThreadLocalRandom.current.nextLong(least, bound)
    }
}
