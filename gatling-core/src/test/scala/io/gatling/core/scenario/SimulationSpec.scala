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
package io.gatling.core.scenario

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification

import io.gatling.core.pause.{ Exponential, PauseProtocol }

@RunWith(classOf[JUnitRunner])
class SimulationSpec extends Specification {
  "setUp" should {
    "set exponential pause protocol" in {
      val simulation = new Simulation {
        setUp().exponentialPauses
      }

      val protocol = simulation._globalProtocols.getProtocol[PauseProtocol]
      protocol.isEmpty must beFalse
      protocol.get.pauseType.isInstanceOf[Exponential.type] must beTrue
    }
  }
}
