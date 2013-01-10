/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import com.excilys.ebi.gatling.core.action.{ Group, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.result.message.RecordEvent.{ END, START }
import com.excilys.ebi.gatling.core.session.Expression

import akka.actor.{ ActorRef, Props }

object GroupBuilder {

	def start(groupName: Expression[String]) = new GroupBuilder(groupName, START)

	def end(groupName: Expression[String]) = new GroupBuilder(groupName, END)
}

class GroupBuilder(groupName: Expression[String], event: String) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new Group(groupName, event, next)))
}