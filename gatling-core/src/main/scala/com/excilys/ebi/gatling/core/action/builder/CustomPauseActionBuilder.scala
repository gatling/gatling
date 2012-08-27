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

import com.excilys.ebi.gatling.core.action.{ system, PauseAction }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

import akka.actor.{ Props, ActorRef }

object CustomPauseActionBuilder {

  /**
   * Creates an initialized CustomPauseActionBuilder with a 1 second delay and a
   * time unit in Seconds.
   */
  def customPauseActionBuilder = new CustomPauseActionBuilder(()=>1L, null)
}

/**
 * Builder for the 'pauseExp' action.  Creates PauseActions for a user with a delay coming from
 * the defined delayGenerator, in milliseconds
 *
 * @constructor create a new ExpPauseActionBuilder
 * @param delayGenerator a function that can be used to generate a delays for the pause action
 * @param next action that will be executed after the generated pause
 */
class CustomPauseActionBuilder(delayGenerator: () => Long, next: ActorRef) extends ActionBuilder {

  /**
   * Adds delayGenerator to builder
   *
   * @param delayGenerator a function that can be used to generate a delays for the pause action
   * @return a new builder with delayGenerator set
   */
  def withDelayGenerator(delayGenerator: () => Long) = new CustomPauseActionBuilder(delayGenerator, next)

  def withNext(next: ActorRef) = new CustomPauseActionBuilder(delayGenerator, next)

  def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
    system.actorOf(Props(new PauseAction(next, delayGenerator)))
  }
}
