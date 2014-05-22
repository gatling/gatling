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
package io.gatling.recorder.export.template

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.model.SimulationModel

object SimulationTemplate {

  def render(model: SimulationModel): Seq[(String, String)] = {

    val output = 
fast"""
import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class ${model.name} extends Simulation {

    setUp(
    
    // the recorder can only record 1 scenario currently
    Scenarios.${model.name}_scenario.inject(atOnceUsers(1))
    
    ).protocols(Protocol.default)
    
    }""".toString()

    List((s"${model.name}_simulation", output))
  }
}
