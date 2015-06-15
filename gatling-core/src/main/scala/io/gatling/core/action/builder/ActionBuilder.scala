/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.action.builder

import io.gatling.core.akka.ActorNames
import io.gatling.core.structure.ScenarioContext

import akka.actor.ActorRef

/**
 * Top level abstraction for components in charge of building Actions.
 * ActionBuilder is what is passed to the DSL exec() method.
 */
trait ActionBuilder extends ActorNames {

  /**
   * @param ctx the scenario context
   * @param next the Action that will be chained with the Action build by this builder
   * @return the resulting Action actor
   */
  def build(ctx: ScenarioContext, next: ActorRef): ActorRef
}
