/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.test

import akka.testkit.{ TestKit, ImplicitSender }
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.config.GatlingConfiguration
import com.typesafe.scalalogging.slf4j.Logging

object ActorSupport extends Logging {

  val consoleOnlyConfig = Map("gatling.data.writers" -> "console")

  def apply(f: TestKit with ImplicitSender => Any): Unit = apply(consoleOnlyConfig)(f)

  def apply(config: Map[String, _])(f: TestKit with ImplicitSender => Any): Unit = synchronized {
    var oldGatlingConfiguration: GatlingConfiguration = null
    try {
      oldGatlingConfiguration = GatlingConfiguration.configuration
      GatlingConfiguration.set(GatlingConfiguration.fakeConfig(config))
      f(new TestKit(GatlingActorSystem.start()) with ImplicitSender)

    } finally {
      GatlingConfiguration.set(oldGatlingConfiguration)
      logger.info("Shutting down GatlingActorSystem")
      GatlingActorSystem.shutdown()
    }
  }

  def of(f: => Any): Unit = apply(_ => f)
}
