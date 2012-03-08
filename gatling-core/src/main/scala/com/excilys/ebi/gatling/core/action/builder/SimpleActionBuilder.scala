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

import scala.annotation.implicitNotFound
import com.excilys.ebi.gatling.core.action.{ SimpleAction, Action }
import com.excilys.ebi.gatling.core.session.Session
import akka.actor.Actor.actorOf
import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

/**
 * SimpleActionBuilder class companion
 */
object SimpleActionBuilder {

	/**
	 * Implicit converter from Session => Unit to a simple action builder containing this function
	 *
	 * @param sessionFunction the function that has to be wrapped into a simple action builder
	 */
	implicit def toSimpleActionBuilder(sessionFunction: Session => Session) = simpleActionBuilder(sessionFunction)

	/**
	 * Function used to create a simple action builder
	 *
	 * @param sessionFunction the function that will be executed by the built simple action
	 */
	def simpleActionBuilder(sessionFunction: Session => Session) = new SimpleActionBuilder(sessionFunction, null)
}

/**
 * This class builds an SimpleAction
 *
 * @constructor creates a SimpleActionBuilder
 * @param sessionFunction the function that will be executed by the simple action
 * @param next the action that will be executed after the simple action built by this builder
 */
class SimpleActionBuilder(sessionFunction: Session => Session, next: ActorRef) extends ActionBuilder {

	def withNext(next: ActorRef) = new SimpleActionBuilder(sessionFunction, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = actorOf(new SimpleAction(sessionFunction, next)).start
}