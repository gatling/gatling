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
package io.gatling.core.pause

import scala.concurrent.duration.Duration
import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.core.session._

sealed abstract class PauseType {
  def generator(duration: Duration): Expression[Long] = generator(duration.expressionSuccess)
  def generator(duration: Expression[Duration]): Expression[Long]
}

object Disabled extends PauseType {
  def generator(duration: Expression[Duration]) = throw new UnsupportedOperationException
}

object Constant extends PauseType {
  def generator(duration: Expression[Duration]) = duration.map(_.toMillis)
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

  def generator(duration: Expression[Duration]) = duration.map {
    duration => (nextValue * duration.toMillis).round
  }
}

case class NormalWithPercentageDuration(stdDev: Double) extends PauseType {

  private val stdDevPercent = stdDev / 100.0

  def generator(duration: Expression[Duration]) = duration.map { d =>
    math.max(0L, ((1 + ThreadLocalRandom.current.nextGaussian * stdDevPercent) * d.toMillis).toLong)
  }
}

case class NormalWithStdDevDuration(stdDev: Duration) extends PauseType {
  def generator(duration: Expression[Duration]) = duration.map { d =>
    math.max(0L, (ThreadLocalRandom.current.nextGaussian * stdDev.toMillis + d.toMillis).toLong)
  }
}

case class Custom(custom: Expression[Long]) extends PauseType {
  def generator(duration: Expression[Duration]) = custom
}

case class UniformPercentage(plusOrMinus: Double) extends PauseType {

  private val plusOrMinusPercent = plusOrMinus / 100.0

  def generator(duration: Expression[Duration]) = duration.map { d =>
    val mean = d.toMillis
    val halfWidth = (mean * plusOrMinusPercent).round
    val least = math.max(0L, mean - halfWidth)
    val bound = mean + halfWidth + 1
    ThreadLocalRandom.current.nextLong(least, bound)
  }
}

case class UniformDuration(plusOrMinus: Duration) extends PauseType {

  private val halfWidth = plusOrMinus.toMillis

  def generator(duration: Expression[Duration]) = duration.map { duration =>
    val mean = duration.toMillis
    val least = math.max(0L, mean - halfWidth)
    val bound = mean + halfWidth + 1
    ThreadLocalRandom.current.nextLong(least, bound)
  }
}
