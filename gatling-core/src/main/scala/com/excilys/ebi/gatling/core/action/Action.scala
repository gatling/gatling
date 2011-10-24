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

import akka.actor.TypedActor

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging

/**
 * This trait represents an Action in Gatling terms.
 *
 * An action is a part of a scenario, the chain of actions IS the scenario
 */
trait Action extends TypedActor with Logging {
	/**
	 * This method is used to send a message to this actor
	 *
	 * @param context The context of the scenario
	 * @return Nothing
	 */
	def execute(context: Context)

	def getUuidAsString = getContext.uuid.toString

	override def toString = this.getClass().getSimpleName()
}