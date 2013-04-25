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
package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.util.ClassSimpleNameToString

import akka.actor.{ Actor, Terminated }
import grizzled.slf4j.Logging

trait BaseActor extends Actor with AkkaDefaults with ClassSimpleNameToString with Logging {

	override def unhandled(message: Any) {
		message match {
			case Terminated(dead) => super.unhandled(message)
			case unknown => throw new IllegalArgumentException("Actor " + this + " doesn't support message " + unknown)
		}
	}
}