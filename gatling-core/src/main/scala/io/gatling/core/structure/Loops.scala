/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import scala.concurrent.duration._

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.action.builder._
import io.gatling.core.session._
import io.gatling.core.session.el.El

private[structure] trait Loops[B] extends Execs[B] {

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def repeat(times: Expression[Int], counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B =
    simpleLoop(
      session => times(session).map(session.loopCounterValue(counterName) < _),
      chain,
      counterName,
      exitASAP = false,
      RepeatLoopType
    )

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def foreach(seq: Expression[Seq[Any]], attributeName: String, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

    val exposeCurrentValue =
      new SessionHookBuilder(session => seq(session).map(seq => session.set(attributeName, seq(session.loopCounterValue(counterName)))), exitable = false)

    simpleLoop(
      session => seq(session).map(_.sizeIs > session.loopCounterValue(counterName)),
      new ChainBuilder(List(exposeCurrentValue)).exec(chain),
      counterName,
      exitASAP = false,
      RepeatLoopType
    )
  }

  // we need these overrides because we can't add an Int => Expression[FiniteDuration]
  // that would clash with Int => Expression[Any] when need for queryParam
  def during(duration: Int)(chain: ChainBuilder): B =
    during(duration.seconds.expressionSuccess)(chain)
  def during(duration: Int, counterName: String)(chain: ChainBuilder): B =
    during(duration.seconds.expressionSuccess, counterName)(chain)
  def during(duration: Int, exitASAP: Boolean)(chain: ChainBuilder): B =
    during(duration.seconds.expressionSuccess, exitASAP = exitASAP)(chain)
  def during(duration: Int, counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    during(duration.seconds.expressionSuccess, counterName, exitASAP)(chain)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def during(duration: Expression[FiniteDuration], counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = true)(chain: ChainBuilder): B =
    clockBasedLoop(
      clock => session => duration(session).map(d => clock.nowMillis - session.loopTimestampValue(counterName) <= d.toMillis),
      chain,
      counterName,
      exitASAP,
      DuringLoopType
    )

  def forever(chain: ChainBuilder): B = forever(UUID.randomUUID.toString)(chain)

  def forever(counterName: String)(chain: ChainBuilder): B =
    simpleLoop(TrueExpressionSuccess, chain, counterName, exitASAP = false, ForeverLoopType)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def asLongAs(condition: Expression[Boolean], counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = false)(chain: ChainBuilder): B =
    simpleLoop(condition, chain, counterName, exitASAP, AsLongAsLoopType)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def doWhile(condition: Expression[Boolean], counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B =
    simpleLoop(condition, chain, counterName, exitASAP = false, DoWhileType)

  private def continueCondition(
      condition: Expression[Boolean],
      duration: Expression[FiniteDuration],
      counterName: String
  ): Clock => Session => Validation[Boolean] =
    clock =>
      session =>
        for {
          durationValue <- duration(session)
          conditionValue <- condition(session)
        } yield conditionValue && clock.nowMillis - session.loopTimestampValue(counterName) <= durationValue.toMillis

  // we need all those overloads because of Scala bug with implicit conversions inference
  def asLongAsDuring[D](condition: String, duration: Int)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def asLongAsDuring[D](condition: String, duration: Int, counterName: String)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(chain)
  def asLongAsDuring[D](condition: String, duration: Int, exitASAP: Boolean)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def asLongAsDuring[D](condition: String, duration: Int, counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(chain)

  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration])(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(chain)
  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    asLongAsDuring(condition.el[Boolean], duration, counterName, exitASAP)(chain)

  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int, counterName: String)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(chain)
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int, exitASAP: Boolean)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int, counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(chain)

  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration])(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(chain)
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: ChainBuilder): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    clockBasedLoop(continueCondition(condition, duration, counterName), chain, counterName, exitASAP, AsLongAsDuringLoopType)

  def doWhileDuring[D](condition: String, duration: Int)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def doWhileDuring[D](condition: String, duration: Int, counterName: String)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(chain)
  def doWhileDuring[D](condition: String, duration: Int, exitASAP: Boolean)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def doWhileDuring[D](condition: String, duration: Int, counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(chain)

  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration])(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(chain)
  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    doWhileDuring(condition.el[Boolean], duration, counterName, exitASAP)(chain)

  def doWhileDuring[D](condition: Expression[Boolean], duration: Int)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def doWhileDuring[D](condition: Expression[Boolean], duration: Int, counterName: String)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(chain)
  def doWhileDuring[D](condition: Expression[Boolean], duration: Int, exitASAP: Boolean)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def doWhileDuring[D](condition: Expression[Boolean], duration: Int, counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(chain)

  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration])(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(chain)
  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(chain)
  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: ChainBuilder): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(chain)
  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B =
    clockBasedLoop(continueCondition(condition, duration, counterName), chain, counterName, exitASAP, DoWhileDuringType)

  private def simpleLoop(
      condition: Expression[Boolean],
      chain: ChainBuilder,
      counterName: String,
      exitASAP: Boolean,
      loopType: LoopType
  ): B =
    exec(new SimpleBooleanConditionLoopBuilder(condition, chain, counterName, exitASAP, loopType))

  private def clockBasedLoop(
      condition: Clock => Expression[Boolean],
      chain: ChainBuilder,
      counterName: String,
      exitASAP: Boolean,
      loopType: LoopType
  ): B =
    exec(new ClockBasedConditionLoopBuilder(condition, chain, counterName, exitASAP, loopType))
}
