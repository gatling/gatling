package com.excilys.ebi.gatling.core.action.builder

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

import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

import akka.actor.{Props, ActorRef}
import com.excilys.ebi.gatling.core.action.{ExpPauseAction, system}


object ExpPauseActionBuilder {

	/**
	 * Creates an initialized ExpPauseActionBuilder no delay and a time unit in Seconds.
	 */
	def expPauseActionBuilder = new ExpPauseActionBuilder(0, TimeUnit.SECONDS, null)
}


/**
 * Builder for ExpPauseAction
 *
 * @constructor create a new ExpPauseActionBuilder
 * @param averageDuration average duration of the generated pause
 * @param timeUnit time unit of the duration of the generated pause
 * @param next action that will be executed after the generated pause
 */
class ExpPauseActionBuilder(averageDuration: Long, timeUnit: TimeUnit, next: ActorRef) extends ActionBuilder {

  /**
   * Adds averageDuration to builder
   *
   * @param averageDuration the minimum duration of the pause
   * @return a new builder with minDuration set
   */
  def withAverageDuration(averageDuration: Long) = new ExpPauseActionBuilder(averageDuration, timeUnit, next)

  /**
   * Adds timeUnit to builder
   *
   * @param timeUnit time unit of the duration
   * @return a new builder with timeUnit set
   */
  def withTimeUnit(timeUnit: TimeUnit) = new ExpPauseActionBuilder(averageDuration, timeUnit, next)

  def withNext(next: ActorRef) = new ExpPauseActionBuilder(averageDuration, timeUnit, next)

  def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new ExpPauseAction(next, averageDuration, timeUnit)))
}
