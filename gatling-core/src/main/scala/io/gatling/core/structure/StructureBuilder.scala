/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.structure

import akka.actor.ActorRef
import io.gatling.core.config.Protocols

/**
 * This trait defines most of the scenario related DSL
 */
trait StructureBuilder[B <: StructureBuilder[B]] extends Execs[B] with Pauses[B] with Feeds[B] with Loops[B] with ConditionalStatements[B] with Errors[B] with Groups[B] {

  private[core] def build(exitPoint: ActorRef, protocols: Protocols): ActorRef =
    actionBuilders.foldLeft(exitPoint) { (actorRef, actionBuilder) =>
      actionBuilder.build(actorRef, protocols)
    }
}
