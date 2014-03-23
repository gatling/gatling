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
package io.gatling.core.akka

import akka.actor.ActorSystem

object GatlingActorSystem {

  var instanceOpt: Option[ActorSystem] = None

  def start() {
    instanceOpt = Some(ActorSystem("GatlingSystem"))
  }

  def instance = instanceOpt match {
    case Some(a) => a
    case None    => throw new UnsupportedOperationException("Gatling Actor system hasn't been started")
  }

  def shutdown() {
    instanceOpt.foreach(_.shutdown())
  }
}
