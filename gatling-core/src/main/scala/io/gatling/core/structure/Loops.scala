/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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
  def repeat(times: Expression[Int], counterName: String = UUID.randomUUID.toString)(chain: Executable, chains: Executable*): B =
    simpleLoop(
      session => times(session).map(session.loopCounterValue(counterName) < _),
      Executable.toChainBuilder(chain, chains),
      counterName,
      exitASAP = false,
      RepeatLoopType
    )

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments", "org.wartremover.warts.SeqApply"))
  def foreach(seq: Expression[Seq[Any]], attributeName: String, counterName: String = UUID.randomUUID.toString)(chain: Executable, chains: Executable*): B = {
    val exposeCurrentValue =
      new SessionHookBuilder(session => seq(session).map(seq => session.set(attributeName, seq(session.loopCounterValue(counterName)))), exitable = false)

    simpleLoop(
      session => seq(session).map(_.sizeIs > session.loopCounterValue(counterName)),
      new ChainBuilder(List(exposeCurrentValue)).exec(Executable.toChainBuilder(chain, chains)),
      counterName,
      exitASAP = false,
      ForeachLoopType
    )
  }

  // we need these overrides because we can't add an Int => Expression[FiniteDuration]
  // that would clash with Int => Expression[Any] when need for queryParam
  def during(duration: Int)(chain: Executable, chains: Executable*): B =
    during(duration.seconds.expressionSuccess)(Executable.toChainBuilder(chain, chains))
  def during(duration: Int, counterName: String)(chain: Executable, chains: Executable*): B =
    during(duration.seconds.expressionSuccess, counterName)(Executable.toChainBuilder(chain, chains))
  def during(duration: Int, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    during(duration.seconds.expressionSuccess, exitASAP = exitASAP)(Executable.toChainBuilder(chain, chains))
  def during(duration: Int, counterName: String, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    during(duration.seconds.expressionSuccess, counterName, exitASAP)(Executable.toChainBuilder(chain, chains))

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def during(duration: Expression[FiniteDuration], counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = true)(
      chain: Executable,
      chains: Executable*
  ): B =
    clockBasedLoop(
      clock => session => duration(session).map(d => clock.nowMillis - session.loopTimestampValue(counterName) <= d.toMillis),
      Executable.toChainBuilder(chain, chains),
      counterName,
      exitASAP,
      DuringLoopType
    )

  def forever(chain: Executable, chains: Executable*): B = forever(UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))

  def forever(counterName: String)(chain: Executable, chains: Executable*): B =
    simpleLoop(TrueExpressionSuccess, Executable.toChainBuilder(chain, chains), counterName, exitASAP = false, ForeverLoopType)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def asLongAs(condition: Expression[Boolean], counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = false)(
      chain: Executable,
      chains: Executable*
  ): B =
    simpleLoop(condition, Executable.toChainBuilder(chain, chains), counterName, exitASAP, AsLongAsLoopType)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def doWhile(condition: Expression[Boolean], counterName: String = UUID.randomUUID.toString)(chain: Executable, chains: Executable*): B =
    simpleLoop(condition, Executable.toChainBuilder(chain, chains), counterName, exitASAP = false, DoWhileType)

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
  def asLongAsDuring[D](condition: String, duration: Int)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: String, duration: Int, counterName: String)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: String, duration: Int, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: String, duration: Int, counterName: String, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(Executable.toChainBuilder(chain, chains))

  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration])(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(
      chain: Executable,
      chains: Executable*
  ): B =
    asLongAsDuring(condition.el[Boolean], duration, counterName, exitASAP)(Executable.toChainBuilder(chain, chains))

  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int, counterName: String)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Int, counterName: String, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(Executable.toChainBuilder(chain, chains))

  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration])(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    asLongAsDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def asLongAsDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(
      chain: Executable,
      chains: Executable*
  ): B =
    clockBasedLoop(continueCondition(condition, duration, counterName), Executable.toChainBuilder(chain, chains), counterName, exitASAP, AsLongAsDuringLoopType)

  def doWhileDuring[D](condition: String, duration: Int)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: String, duration: Int, counterName: String)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: String, duration: Int, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: String, duration: Int, counterName: String, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(Executable.toChainBuilder(chain, chains))

  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration])(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: String, duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(
      chain: Executable,
      chains: Executable*
  ): B =
    doWhileDuring(condition.el[Boolean], duration, counterName, exitASAP)(Executable.toChainBuilder(chain, chains))

  def doWhileDuring[D](condition: Expression[Boolean], duration: Int)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: Expression[Boolean], duration: Int, counterName: String)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: Expression[Boolean], duration: Int, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: Expression[Boolean], duration: Int, counterName: String, exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration.seconds.expressionSuccess, counterName, exitASAP)(Executable.toChainBuilder(chain, chains))

  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration])(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, counterName, exitASAP = true)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], exitASAP: Boolean)(chain: Executable, chains: Executable*): B =
    doWhileDuring(condition, duration, UUID.randomUUID.toString, exitASAP)(Executable.toChainBuilder(chain, chains))
  def doWhileDuring[D](condition: Expression[Boolean], duration: Expression[FiniteDuration], counterName: String, exitASAP: Boolean)(
      chain: Executable,
      chains: Executable*
  ): B =
    clockBasedLoop(continueCondition(condition, duration, counterName), Executable.toChainBuilder(chain, chains), counterName, exitASAP, DoWhileDuringType)

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
