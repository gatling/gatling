/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.structure

import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{ Duration, DurationLong }
import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.core.action.builder.{ PaceBuilder, PauseBuilder, RendezVousBuilder }
import io.gatling.core.session.{ Expression, ExpressionWrapper, Session }
import io.gatling.core.session.el.EL
import io.gatling.core.validation.SuccessWrapper

trait Pauses[B] extends Execs[B] {

  private def durationExpression(duration: String, unit: TimeUnit): Expression[Duration] = {
    val durationValue = duration.el[Int]
    durationValue(_).map(i => Duration(i, unit))
  }

  private def durationExpression(min: Duration, max: Duration): Expression[Duration] =
    if (min == max)
      min.expression

    else {
      val minMillis = min.toMillis
      val maxMillis = max.toMillis
      (session: Session) => (ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis).success
    }

  private def durationExpression(min: String, max: String, unit: TimeUnit): Expression[Duration] = {
    val minExpression = min.el[Int].map(Duration(_, unit))
    val maxExpression = max.el[Int].map(Duration(_, unit))

    durationExpression(minExpression, maxExpression)
  }

  private def durationExpression(min: Expression[Duration], max: Expression[Duration]): Expression[Duration] =
    (session: Session) =>
      for {
        min <- min(session)
        max <- max(session)
      } yield {
        if (min == max)
          min
        else {
          val minMillis = min.toMillis
          val maxMillis = max.toMillis
          ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis
        }
      }

  /**
   * Method used to define a pause based on a duration defined in the session
   *
   * @param duration Expression that when resolved, provides the pause duration
   * @return a new builder with a pause added to its actions
   */
  def pause(duration: Duration): B = pause(duration, false)
  def pause(duration: Duration, force: Boolean): B = pause(duration.expression, force)

  def pause(duration: String): B = pause(duration, false)
  def pause(duration: String, force: Boolean): B = pause(duration, TimeUnit.SECONDS, force)
  def pause(duration: String, unit: TimeUnit): B = pause(duration, unit, false)
  def pause(duration: String, unit: TimeUnit, force: Boolean): B = pause(durationExpression(duration, unit), force)

  def pause(min: Duration, max: Duration): B = pause(min, max, false)
  def pause(min: Duration, max: Duration, force: Boolean): B = pause(durationExpression(min, max), force)

  def pause(min: String, max: String, unit: TimeUnit): B = pause(min, max, unit, false)
  def pause(min: String, max: String, unit: TimeUnit, force: Boolean): B = pause(durationExpression(min, max, unit), force)

  def pause(min: Expression[Duration], max: Expression[Duration]): B = pause(min, max, false)
  def pause(min: Expression[Duration], max: Expression[Duration], force: Boolean): B = pause(durationExpression(min, max), force)

  def pause(duration: Expression[Duration]): B = pause(duration, false)
  def pause(duration: Expression[Duration], force: Boolean): B = exec(new PauseBuilder(duration, force))

  def pace(duration: String, unit: TimeUnit = TimeUnit.SECONDS): B = pace(durationExpression(duration, unit))

  def pace(min: Duration, max: Duration): B = pace(durationExpression(min, max))

  def pace(min: String, max: String, unit: TimeUnit): B = pace(durationExpression(min, max, unit))

  def pace(min: Expression[Duration], max: Expression[Duration]): B = pace(durationExpression(min, max))

  def pace(duration: Expression[Duration]): B = pace(duration, UUID.randomUUID.toString)

  def pace(duration: Expression[Duration], counter: String): B = exec(new PaceBuilder(duration, counter))

  def rendezVous(users: Int): B = exec(new RendezVousBuilder(users))
}
