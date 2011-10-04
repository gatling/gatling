/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.action
import com.excilys.ebi.gatling.core.context.Context

/**
 * This class represents a conditional Action
 *
 * @constructor creates an IfAction
 * @param testFunction this function is the condition that decides of what action to execute next
 * @param nextTrue chain of actions executed if testFunction evaluates to true
 * @param nextFalse chain of actions executed if testFunction evaluates to false
 * @param nextAfter chain of actions executed if testFunction evaluates to false and nextFalse equals None
 */
class IfAction(testFunction: Context => Boolean, nextTrue: Action, nextFalse: Option[Action], nextAfter: Action) extends Action {

  /**
   * Evaluates the testFunction and if true executes the first action of nextTrue
   * else it executes the first action of nextFalse.
   *
   * If there is no nextFalse, then, nextAfter is executed
   *
   * @param context Context for current user
   * @return Nothing
   */
  def execute(context: Context) = {
    if (testFunction.apply(context))
      nextTrue.execute(context)
    else
      nextFalse.getOrElse(nextAfter).execute(context)
  }
}