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

package io.gatling.core.structure

import java.util.UUID
import java.util.concurrent.{ ThreadLocalRandom, TimeUnit }

import scala.concurrent.duration._

import io.gatling.commons.validation._
import io.gatling.core.action.builder.{ PaceBuilder, PauseBuilder, RendezVousBuilder }
import io.gatling.core.pause.PauseType
import io.gatling.core.session._
import io.gatling.core.session.el.El

private[structure] trait Pauses[B] extends Execs[B] {

  private def durationExpression(duration: String, unit: Option[TimeUnit]): Expression[FiniteDuration] =
    unit match {
      case Some(u) => duration.el[Long].map(FiniteDuration(_, u))
      case _       => duration.el[FiniteDuration]
    }

  private def durationExpression(min: FiniteDuration, max: FiniteDuration): Expression[FiniteDuration] =
    if (min == max)
      min.expressionSuccess
    else {
      val minMillis = min.toMillis
      val maxMillis = max.toMillis
      _ => (ThreadLocalRandom.current.nextLong(minMillis, maxMillis).millis).success
    }

  private def durationExpression(min: String, max: String, unit: Option[TimeUnit]): Expression[FiniteDuration] = {
    val minExpression = durationExpression(min, unit)
    val maxExpression = durationExpression(max, unit)

    durationExpression(minExpression, maxExpression)
  }

  private def durationExpression(min: Expression[FiniteDuration], max: Expression[FiniteDuration]): Expression[FiniteDuration] =
    (session: Session) =>
      for {
        min <- min(session)
        max <- max(session)
      } yield {
        if (min == max) {
          min
        } else {
          val minMillis = min.toMillis
          val maxMillis = max.toMillis
          ThreadLocalRandom.current.nextLong(minMillis, maxMillis).millis
        }
      }

  /**
   * Method used to define a pause based on a duration defined in the session
   *
   * @param duration Expression that when resolved, provides the pause duration
   * @return a new builder with a pause added to its actions
   */
  def pause(duration: FiniteDuration): B = pause(duration, None)
  def pause(duration: FiniteDuration, force: PauseType): B = pause(duration, Some(force))
  private def pause(duration: FiniteDuration, force: Option[PauseType]): B = pause(duration.expressionSuccess, force)

  def pause(duration: String): B = pause(duration, None, None)
  def pause(duration: String, force: PauseType): B = pause(duration, None, Some(force))
  def pause(duration: String, unit: TimeUnit): B = pause(duration, Some(unit), None)
  def pause(duration: String, unit: TimeUnit, force: PauseType): B = pause(duration, Some(unit), Some(force))
  private def pause(duration: String, unit: Option[TimeUnit], force: Option[PauseType]): B = pause(durationExpression(duration, unit), force)

  def pause(min: FiniteDuration, max: FiniteDuration): B = pause(min, max, None)
  def pause(min: FiniteDuration, max: FiniteDuration, force: PauseType): B = pause(min, max, Some(force))
  private def pause(min: FiniteDuration, max: FiniteDuration, force: Option[PauseType]): B = pause(durationExpression(min, max), force)

  def pause(min: String, max: String, unit: TimeUnit): B = pause(min, max, unit, None)
  def pause(min: String, max: String, unit: TimeUnit, force: PauseType): B = pause(min, max, unit, Some(force))
  private def pause(min: String, max: String, unit: TimeUnit, force: Option[PauseType]): B = pause(durationExpression(min, max, Some(unit)), force)

  def pause(min: Expression[FiniteDuration], max: Expression[FiniteDuration]): B = pause(min, max, None)
  def pause(min: Expression[FiniteDuration], max: Expression[FiniteDuration], force: PauseType): B = pause(min, max, Some(force))
  private def pause(min: Expression[FiniteDuration], max: Expression[FiniteDuration], force: Option[PauseType]): B = pause(durationExpression(min, max), force)

  def pause(duration: Expression[FiniteDuration]): B = pause(duration, None)
  def pause(duration: Expression[FiniteDuration], force: PauseType): B = pause(duration, Some(force))
  private def pause(duration: Expression[FiniteDuration], force: Option[PauseType]): B = exec(new PauseBuilder(duration, force))

  def pace(duration: FiniteDuration): B = pace(duration.expressionSuccess)
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def pace(duration: String, unit: TimeUnit = TimeUnit.SECONDS): B = pace(durationExpression(duration, Some(unit)))
  def pace(min: FiniteDuration, max: FiniteDuration): B = pace(durationExpression(min, max))
  def pace(min: String, max: String, unit: TimeUnit): B = pace(durationExpression(min, max, Some(unit)))
  def pace(min: Expression[FiniteDuration], max: Expression[FiniteDuration]): B = pace(durationExpression(min, max))
  def pace(duration: Expression[FiniteDuration]): B = pace(duration, UUID.randomUUID.toString)
  def pace(duration: Expression[FiniteDuration], counter: String): B = exec(new PaceBuilder(duration, counter))

  def rendezVous(users: Int): B = exec(new RendezVousBuilder(users))
}
