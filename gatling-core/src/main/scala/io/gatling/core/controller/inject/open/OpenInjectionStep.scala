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

package io.gatling.core.controller.inject.open

import java.util.Random

import scala.collection.AbstractIterator
import scala.concurrent.duration._
import scala.math.abs

import io.gatling.core.util.Shard

sealed trait OpenInjectionStep extends Product with Serializable {

  /**
   * Iterator of time deltas in between any injected user and the beginning of the simulation
   */
  private[inject] def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration]

  /**
   * Number of users to inject
   */
  private[inject] def users: Long

  private[inject] def duration: FiniteDuration
}

abstract class InjectionIterator(durationInSeconds: Int) extends AbstractIterator[FiniteDuration] {

  private var thisSecond: Int = -1
  private var thisSecondIterator: Iterator[FiniteDuration] = Iterator.empty
  private var finished = finishedAfterMovingToNextBatch()

  protected def thisSecondUsers(thisSecondParam: Int): Int

  // only called if !finished
  private def finishedAfterMovingToNextBatch(): Boolean = {
    var result: Option[Boolean] = None
    do {
      thisSecond += 1

      if (thisSecond == durationInSeconds) {
        thisSecondIterator = Iterator.empty
        result = Some(true)

      } else {
        val users = thisSecondUsers(thisSecond)

        if (users > 0) {
          thisSecondIterator = Shard
            .shards(users, 1000)
            .zipWithIndex
            .flatMap { case (millisUsers, millis) =>
              if (millisUsers > 0)
                Iterator.fill(millisUsers.toInt)((thisSecond * 1000 + millis).milliseconds)
              else
                Iterator.empty
            }
          result = Some(false)
        }
      }
    } while (!thisSecondIterator.hasNext && result.isEmpty)
    result.getOrElse(true)
  }

  override def hasNext: Boolean =
    if (thisSecondIterator.hasNext) {
      true
    } else if (finished) {
      false
    } else {
      finished = finishedAfterMovingToNextBatch()
      !finished
    }

  override def next(): FiniteDuration = thisSecondIterator.next()

  override def toString(): String = "non-printable iterator"
}

/**
 * Ramp a given number of users over a given duration
 */
final case class RampOpenInjection private[inject] (users: Long, duration: FiniteDuration) extends OpenInjectionStep {

  require(users >= 0, s"users ($users) must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be >= 0")

  override def chain(chained: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    if (users == 0) {
      NothingForOpenInjection(duration).chain(chained)

    } else if (duration == Duration.Zero) {
      AtOnceOpenInjection(users).chain(chained)

    } else {
      val durationInSeconds = duration.toSeconds.toInt

      new InjectionIterator(durationInSeconds) {
        override protected def thisSecondUsers(thisSecondParam: Int): Int = Shard.shard(users, thisSecondParam, durationInSeconds).length
      } ++ chained.map(_ + duration)
    }
}

/**
 * Inject users at constant rate : an other expression of a RampInjection
 */
final case class ConstantRateOpenInjection private[inject] (rate: Double, duration: FiniteDuration) extends OpenInjectionStep {

  require(rate >= 0, s"rate ($rate) must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be >= 0")
  require(!(rate > 0 && duration == Duration.Zero), s"can't inject a non 0 rate ($rate) for a 0 duration")

  override private[inject] val users: Long = (duration.toSeconds * rate).round

  def randomized: OpenInjectionStep = PoissonOpenInjection(duration, rate, rate)

  override private[inject] def chain(chained: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    if (rate == 0) {
      NothingForOpenInjection(duration).chain(chained)
    } else {
      RampOpenInjection(users, duration).chain(chained)
    }
}

/**
 * Don't injection any user for a given duration
 */
final case class NothingForOpenInjection private[inject] (duration: FiniteDuration) extends OpenInjectionStep {

  require(duration >= Duration.Zero, s"duration ($duration) must be >= 0")

  override private[inject] def chain(chained: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    if (duration == Duration.Zero) {
      chained
    } else {
      chained.map(_ + duration)
    }

  override private[inject] val users: Long = 0
}

/**
 * Inject all the users at once
 */
final case class AtOnceOpenInjection private[inject] (users: Long) extends OpenInjectionStep {

  require(users >= 0, s"users ($users) must be >= 0")

  override private[inject] def chain(chained: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    if (users == 0) {
      chained
    } else {
      new AbstractIterator[FiniteDuration] {

        private var i: Long = 0L

        override def hasNext: Boolean = i < users

        override def next(): FiniteDuration = {
          if (!hasNext) throw new NoSuchElementException
          i += 1
          Duration.Zero
        }
      } ++ chained
    }

  override private[inject] def duration: FiniteDuration = Duration.Zero
}

/**
 * @param startRate Initial injection rate in users/seconds
 * @param endRate Final injection rate in users/seconds
 * @param duration Injection duration
 */
final case class RampRateOpenInjection private[inject] (startRate: Double, endRate: Double, duration: FiniteDuration) extends OpenInjectionStep {

  require(startRate >= 0.0 && endRate >= 0.0, s"injection rates ($startRate, $endRate) must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be >= 0")
  require(!((startRate > 0 || endRate > 0) && duration == Duration.Zero), s"can't inject non 0 rates ($startRate, $endRate) for a 0 duration")

  override private[inject] val users: Long = ((startRate + (endRate - startRate) / 2) * duration.toSeconds).toLong

  def randomized: OpenInjectionStep = PoissonOpenInjection(duration, startRate, endRate)

  override private[inject] def chain(chained: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    if (startRate == 0 && endRate == 0) {
      NothingForOpenInjection(duration).chain(chained)

    } else {
      val durationInSeconds = duration.toSeconds.toInt
      val a = (BigDecimal(endRate) - startRate) / (2 * durationInSeconds)
      var pendingFraction = BigDecimal(0.0)

      new InjectionIterator(durationInSeconds) {

        override protected def thisSecondUsers(thisSecondParam: Int): Int = {
          val thisSecondUsersBigDecimal = a * (2 * thisSecondParam + 1) + startRate + pendingFraction
          val thisSecondUsersIntValue = thisSecondUsersBigDecimal.setScale(10, BigDecimal.RoundingMode.HALF_UP).intValue
          pendingFraction = thisSecondUsersBigDecimal - thisSecondUsersIntValue
          thisSecondUsersIntValue
        }
      } ++ chained.map(_ + duration)
    }
}

/**
 * Injection rate following a Heaviside distribution function
 *
 * {{{
 * numberOfInjectedUsers(t) = u(t)
 *                          = ∫δ(t)
 *                          = Heaviside(t)
 *                          = 1/2 + 1/2*erf(k*t)
 *                          // (good numerical approximation)
 * }}}
 */
final case class HeavisideOpenInjection private[inject] (private[inject] val users: Long, duration: FiniteDuration) extends OpenInjectionStep {

  require(users >= 0, s"users ($users) must be >= 0")
  require(duration >= Duration.Zero, s"Duration ($duration) must be >= 0")

  override private[inject] def chain(chained: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    if (users == 0) {
      NothingForOpenInjection(duration).chain(chained)

    } else if (duration == Duration.Zero) {
      AtOnceOpenInjection(users).chain(chained)

    } else {
      def heavisideInv(u: Long): Double = {
        val x = u.toDouble / (users + 2)
        Erf.erfinv(2 * x - 1)
      }

      val t0 = abs(heavisideInv(1))
      val d = t0 * 2
      val k = duration.toMillis / d

      new AbstractIterator[FiniteDuration] {

        private var i: Long = 0L

        override def hasNext: Boolean = i < users

        override def next(): FiniteDuration = {
          if (!hasNext) throw new NoSuchElementException
          i += 1
          val t = heavisideInv(i)
          (k * (t + t0)).toLong.milliseconds
        }
      } ++ chained.map(_ + duration)
    }
}

object PoissonOpenInjection {
  private[inject] def apply(duration: FiniteDuration, startRate: Double, endRate: Double): PoissonOpenInjection =
    new PoissonOpenInjection(duration, startRate, endRate, System.nanoTime)
}

/**
 * Inject users following a Poisson random process, with a ramped injection rate.
 *
 * A Poisson process models users arriving at a page randomly. You can specify the rate
 * that users arrive at, and this rate can ramp-up.
 *
 * Note that since this injector has an element of randomness, the total number of users
 * may vary from run to run, depending on the seed.
 *
 * @param duration the length of time this injector should run for
 * @param startRate initial injection rate for users
 * @param endRate final injection rate for users
 * @param seed a seed for the randomization. If the same seed is re-used, the same timings will be obtained
 */
final case class PoissonOpenInjection private[inject] (duration: FiniteDuration, startRate: Double, endRate: Double, seed: Long) extends OpenInjectionStep {

  require(startRate >= 0.0 && endRate >= 0.0, s"injection rates ($startRate, $endRate) must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be > 0")
  require(!((startRate > 0 || endRate > 0) && duration == Duration.Zero), s"can't inject non 0 rates ($startRate, $endRate) for a 0 duration")

  override private[inject] val users: Long = chain(Iterator.empty).size

  override private[inject] def chain(chained: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    if (startRate == 0 && endRate == 0) {
      NothingForOpenInjection(duration).chain(chained)

    } else {
      val durationSecs = duration.toSeconds
      val rand = new Random(seed)

      // Uses Lewis and Shedler's thinning algorithm: https://www.math.fsu.edu/~ychen/research/Thinning%20algorithm.pdf chapter 3.2.2
      val maxLambda = math.max(startRate, endRate)
      def shouldKeep(d: Double) = {
        val actualLambda = startRate + (endRate - startRate) * d / durationSecs
        rand.nextDouble() < actualLambda / maxLambda
      }

      val rawIntervals = Iterator.continually(-math.log(rand.nextDouble()) / maxLambda)

      rawIntervals
        .scanLeft(0.0)(_ + _) // Rolling sum
        .drop(1) // Throw away first value of 0.0. It is not random, but a quirk of using scanLeft
        .takeWhile(_ < durationSecs)
        .filter(shouldKeep)
        .map(_.seconds) ++ chained.map(_ + duration)
    }
}

sealed trait CompositeOpenInjectionStepLike extends OpenInjectionStep {
  private[inject] def composite: CompositeOpenInjectionStep
}

final case class IncreasingUsersPerSecCompositeStep private[inject] (
    usersPerSec: Double,
    nbOfSteps: Int,
    duration: FiniteDuration,
    startingUsers: Double,
    rampDuration: FiniteDuration
) extends CompositeOpenInjectionStepLike {
  def startingFrom(startingUsers: Double): IncreasingUsersPerSecCompositeStep = this.copy(startingUsers = startingUsers)
  def separatedByRampsLasting(duration: FiniteDuration): IncreasingUsersPerSecCompositeStep = this.copy(rampDuration = duration)

  override private[inject] lazy val composite = {
    val injectionSteps =
      List.range(0, nbOfSteps).flatMap { stepIdx =>
        if (rampDuration > Duration.Zero) {
          if (startingUsers == 0) {
            // (ramp, level)*
            val rampStartRate = stepIdx * usersPerSec
            val levelRate = (stepIdx + 1) * usersPerSec
            RampRateOpenInjection(rampStartRate, levelRate, rampDuration) :: ConstantRateOpenInjection(levelRate, duration) :: Nil

          } else {
            // (level, ramp)* + level
            val levelRate = stepIdx * usersPerSec + startingUsers
            val level = ConstantRateOpenInjection(levelRate, duration)
            if (stepIdx == nbOfSteps - 1) {
              level :: Nil
            } else {
              val rampEndRate = (stepIdx + 1) * usersPerSec + startingUsers
              level :: RampRateOpenInjection(levelRate, rampEndRate, rampDuration) :: Nil
            }
          }
        } else {
          // only levels
          val levelRate = stepIdx * usersPerSec + startingUsers
          ConstantRateOpenInjection(levelRate, duration) :: Nil
        }
      }

    CompositeOpenInjectionStep(injectionSteps)
  }

  override private[inject] def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    composite.chain(iterator)

  override private[inject] def users: Long =
    composite.users
}

private[inject] final case class CompositeOpenInjectionStep private[inject] (steps: List[OpenInjectionStep]) extends OpenInjectionStep {

  override private[inject] def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration] =
    steps.foldRight(iterator) { case (injectionStep, acc) =>
      injectionStep.chain(acc)
    }

  override private[inject] def users: Long = steps.map(_.users).sum

  override private[inject] def duration: FiniteDuration = steps.foldLeft(Duration.Zero)((acc, step) => acc.plus(step.duration))
}
