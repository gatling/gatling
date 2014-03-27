/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.test

import org.specs2.specification.Scope

import akka.testkit.TestKitBase
import io.gatling.core.akka.GatlingActorSystem
import org.specs2.mutable.After

object ActorSupport {
  def gatlingActorSystem = {
    GatlingActorSystem.instanceOpt match {
      case None =>
        GatlingActorSystem.start()
        GatlingActorSystem.instance
      case _ =>
        ??? // No supported - if ActorSystem wasn't shut down cleanly, we have a problem
    }
  }
}

class ActorSupport extends { val system = ActorSupport.gatlingActorSystem } with TestKitBase with After with Scope {
  implicit def self = testActor // Copied from ImplicitSender - doesn't work with TestKitBase

  override def after: Any = {
    GatlingActorSystem.shutdown()
    GatlingActorSystem.instanceOpt = None
  }
}
