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
package io.gatling.core.structure

import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{ Duration, DurationLong }
import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.commons.validation._
import io.gatling.core.action.builder.{ PaceBuilder, PauseBuilder, RendezVousBuilder }
import io.gatling.core.pause.PauseType
import io.gatling.core.session._
import io.gatling.core.session.el.El

trait Pauses[B] extends Execs[B] {

  private def durationExpression(duration: String, unit: TimeUnit): Expression[Duration] = {
    val durationValue = duration.el[Int]
    durationValue(_).map(i => Duration(i, unit))
  }

  private def durationExpression(min: Duration, max: Duration): Expression[Duration] =
    if (min == max)
      min.expressionSuccess

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
  def pause(duration: Duration): B = pause(duration, None)
  def pause(duration: Duration, force: PauseType): B = pause(duration, Some(force))
  private def pause(duration: Duration, force: Option[PauseType]): B = pause(duration.expressionSuccess, force)

  def pause(duration: String): B = pause(duration, TimeUnit.SECONDS, None)
  def pause(duration: String, force: PauseType): B = pause(duration, TimeUnit.SECONDS, Some(force))
  def pause(duration: String, unit: TimeUnit): B = pause(duration, unit, None)
  def pause(duration: String, unit: TimeUnit, force: PauseType): B = pause(duration, unit, Some(force))
  private def pause(duration: String, unit: TimeUnit, force: Option[PauseType]): B = pause(durationExpression(duration, unit), force)

  def pause(min: Duration, max: Duration): B = pause(min, max, None)
  def pause(min: Duration, max: Duration, force: PauseType): B = pause(min, max, Some(force))
  private def pause(min: Duration, max: Duration, force: Option[PauseType]): B = pause(durationExpression(min, max), force)

  def pause(min: String, max: String, unit: TimeUnit): B = pause(min, max, unit, None)
  def pause(min: String, max: String, unit: TimeUnit, force: PauseType): B = pause(min, max, unit, Some(force))
  private def pause(min: String, max: String, unit: TimeUnit, force: Option[PauseType]): B = pause(durationExpression(min, max, unit), force)

  def pause(min: Expression[Duration], max: Expression[Duration]): B = pause(min, max, None)
  def pause(min: Expression[Duration], max: Expression[Duration], force: PauseType): B = pause(min, max, Some(force))
  private def pause(min: Expression[Duration], max: Expression[Duration], force: Option[PauseType]): B = pause(durationExpression(min, max), force)

  def pause(duration: Expression[Duration]): B = pause(duration, None)
  def pause(duration: Expression[Duration], force: PauseType): B = pause(duration, Some(force))
  private def pause(duration: Expression[Duration], force: Option[PauseType]): B = exec(new PauseBuilder(duration, force))

  def pace(duration: Duration): B = pace(duration.expressionSuccess)
  def pace(duration: String, unit: TimeUnit = TimeUnit.SECONDS): B = pace(durationExpression(duration, unit))

  def pace(min: Duration, max: Duration): B = pace(durationExpression(min, max))
  def pace(min: String, max: String, unit: TimeUnit): B = pace(durationExpression(min, max, unit))
  def pace(min: Expression[Duration], max: Expression[Duration]): B = pace(durationExpression(min, max))

  def pace(duration: Expression[Duration]): B = pace(duration, UUID.randomUUID.toString)
  def pace(duration: Expression[Duration], counter: String): B = exec(new PaceBuilder(duration, counter))

  def rendezVous(users: Int): B = exec(new RendezVousBuilder(users))
}
