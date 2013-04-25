/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.{ GroupAction, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.result.message.RecordEvent.{ END, START }
import com.excilys.ebi.gatling.core.session.EvaluatableString

import akka.actor.{ ActorRef, Props }

object GroupActionBuilder {

	def start(groupName: EvaluatableString) = new GroupActionBuilder(groupName, START, null)

	def end(groupName: EvaluatableString) = new GroupActionBuilder(groupName, END, null)
}

class GroupActionBuilder(groupName: EvaluatableString, event: String, next: ActorRef) extends ActionBuilder {

	def withNext(next: ActorRef) = new GroupActionBuilder(groupName, event, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new GroupAction(groupName, event, next)))

}