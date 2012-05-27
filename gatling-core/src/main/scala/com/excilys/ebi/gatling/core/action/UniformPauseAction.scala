/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.action

import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.util.NumberHelper.getRandomLong

import akka.actor.ActorRef

/**
 * An action for "pausing" a user (ie: think time)
 *
 * @constructor creates a UniformPauseAction
 * @param nextAction action that will be executed after the pause duration
 * @param minDuration minimum duration of the pause
 * @param maxDuration maximum duration of the pause
 * @param timeUnit time unit of the duration
 */
class UniformPauseAction(nextAction: ActorRef, minDuration: Long, maxDuration: Option[Long], timeUnit: TimeUnit)
  extends PauseAction {

  val next: ActorRef = nextAction;
  val minDurationInMillis = TimeUnit.MILLISECONDS.convert(minDuration, timeUnit)
  val maxDurationInMillis = maxDuration.map(TimeUnit.MILLISECONDS.convert(_, timeUnit))


  def generateDelayInMillis: Long = {
    maxDurationInMillis.map(getRandomLong(minDurationInMillis, _)).getOrElse(minDurationInMillis)
  }
}
