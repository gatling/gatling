/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.core.action.builder.{ Executable, ExitHereBuilder, StopLoadGeneratorBuilder, TryMaxBuilder }
import io.gatling.core.session._

private[structure] trait Errors[B] extends Execs[B] {
  import SessionPrivateAttributes._

  def exitBlockOnFail(chain: Executable, chains: Executable*): B = tryMax(1.expressionSuccess)(chain, chains: _*)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def tryMax(times: Expression[Int], counterName: String = generateUniquePrivateAttribute("tryMax"))(chain: Executable, chains: Executable*): B =
    exec(new TryMaxBuilder(times, counterName, Executable.toChainBuilder(chain, chains)))

  def exitHereIf(condition: Expression[Boolean]): B = exec(new ExitHereBuilder(condition))

  def exitHere: B = exitHereIf(TrueExpressionSuccess)

  def exitHereIfFailed: B = exec(ExitHereBuilder())

  def stopLoadGenerator(message: Expression[String]): B = stopLoadGeneratorIf(message, TrueExpressionSuccess)

  def stopLoadGeneratorIf(message: Expression[String], condition: Expression[Boolean]): B = exec(
    new StopLoadGeneratorBuilder(message, condition, crash = false)
  )

  def crashLoadGenerator(message: Expression[String]): B = crashLoadGeneratorIf(message, TrueExpressionSuccess)

  def crashLoadGeneratorIf(message: Expression[String], condition: Expression[Boolean]): B = exec(
    new StopLoadGeneratorBuilder(message, condition, crash = true)
  )
}
