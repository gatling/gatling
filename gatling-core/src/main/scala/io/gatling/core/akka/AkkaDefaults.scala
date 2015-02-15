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

import java.util.concurrent.atomic.AtomicLong

import akka.pattern.AskSupport

object AkkaDefaults {
  val IdGen = new AtomicLong()
}

trait AkkaDefaults extends AskSupport {

  import AkkaDefaults._

  implicit def system = GatlingActorSystem.instance
  implicit def dispatcher = system.dispatcher
  implicit def scheduler = system.scheduler

  def actorName(base: String) = base + "-" + IdGen.incrementAndGet
}
