/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.EndAction

import akka.actor.TypedActor

import java.util.concurrent.CountDownLatch

object EndActionBuilder {
  /**
   * Creates a new EndActionBuilder
   *
   * @param countDownLatch the countdown latch that will stop the simulation execution
   * @return An EndActionBuilder ready to use
   */
  def endActionBuilder(latch: CountDownLatch) = new EndActionBuilder(latch)
}

/**
 * Builder for EndAction
 *
 * @constructor create an EndActionBuilder with its countdown slatch
 * @param latch The CountDownLatch that will stop the simulation
 */
class EndActionBuilder(val latch: CountDownLatch) extends AbstractActionBuilder {

  def build: Action = {
    TypedActor.newInstance(classOf[Action], new EndAction(latch))
  }

  def withNext(next: Action): AbstractActionBuilder = this

  def inGroups(groups: List[String]) = this

  override def toString = "End"
}
