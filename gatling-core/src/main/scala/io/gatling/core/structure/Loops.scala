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

import scala.concurrent.duration.Duration

import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.core.action.builder._
import io.gatling.core.session._
import io.gatling.core.structure.ChainBuilder.chainOf

trait Loops[B] extends Execs[B] {

  def repeat(times: Expression[Int], counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

    val continueCondition = (session: Session) => times(session).map(session.loopCounterValue(counterName) < _)

    asLongAs(continueCondition, counterName, exitASAP = false, RepeatLoopType)(chain)
  }

  def foreach(seq: Expression[Seq[Any]], attributeName: String, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

    val exposeCurrentValue = (session: Session) => seq(session).map(seq => session.set(attributeName, seq(session.loopCounterValue(counterName))))
    val continueCondition = (session: Session) => seq(session).map(_.size > session.loopCounterValue(counterName))

    asLongAs(continueCondition, counterName, exitASAP = false, ForeachLoopType)(chainOf(new SessionHookBuilder(exposeCurrentValue)).exec(chain))
  }

  def during(duration: Duration, counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = true)(chain: ChainBuilder): B =
    during(duration.expressionSuccess, counterName, exitASAP)(chain)

  def during(duration: Expression[Duration], counterName: String, exitASAP: Boolean)(chain: ChainBuilder): B = {

    val continueCondition = (session: Session) => duration(session).map(d => nowMillis - session.loopTimestampValue(counterName) <= d.toMillis)

    asLongAs(continueCondition, counterName, exitASAP, DuringLoopType)(chain)
  }

  def forever(chain: ChainBuilder): B = forever(UUID.randomUUID.toString, exitASAP = false)(chain)

  def forever(counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = false)(chain: ChainBuilder): B =
    asLongAs(TrueExpressionSuccess, counterName, exitASAP, ForeverLoopType)(chain)

  def asLongAs(condition: Expression[Boolean], counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = false, loopType: LoopType = AsLongAsLoopType)(chain: ChainBuilder): B =
    exec(new LoopBuilder(condition, chain, counterName, exitASAP, loopType))
}
