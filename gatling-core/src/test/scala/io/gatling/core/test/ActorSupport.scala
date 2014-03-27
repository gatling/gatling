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

import org.specs2.specification.Fixture

import akka.testkit.{ TestKit, ImplicitSender }
import io.gatling.core.akka.GatlingActorSystem
import com.typesafe.scalalogging.slf4j.Logging
import org.specs2.execute.AsResult

object ActorSupport extends Fixture[TestKit with ImplicitSender] with Logging {
  def apply[R: AsResult](f: TestKit with ImplicitSender => R) = GatlingActorSystem.synchronized {
    try {
      AsResult(f(new TestKit(
        GatlingActorSystem.instanceOpt match {
          case None =>
            logger.info("Starting GatlingActorSystem")
            GatlingActorSystem.start()
            GatlingActorSystem.instance
          case _ =>
            throw new RuntimeException("GatlingActorSystem already started!")
        }) with ImplicitSender))
    } finally {
      logger.info("Shutting down GatlingActorSystem")
      GatlingActorSystem.instance.shutdown() // Call to instance is defensive - ensure that double-shutdown doesn't pass silently
      GatlingActorSystem.instanceOpt = None
    }
  }
}
