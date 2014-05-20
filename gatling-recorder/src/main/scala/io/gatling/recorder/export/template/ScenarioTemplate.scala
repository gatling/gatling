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

object ScenarioTemplate {

  def render(model: SimulationModel): Seq[(String, String)] = {

      def renderScenarios = {

        val navigations = model.getNavigations.map {

          navigation =>
            {
              val nav_name = navigation._2.name
              fast"""Navigations.$nav_name,\n\t\t"""
            }.mkFastring("")

        }.mkFastring("")

        fast"""\n\tval ${model.name}_scenario = scenario("${model.name} scenario").exec(   $navigations  )"""

      }.mkFastring("")

    val output = fast"""// package TODO
    
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

object Scenarios {

    // scenario - sequence of steps
    
    $renderScenarios
}
""".toString()

    List((s"${model.name}_scenarios", output))
  }
}
