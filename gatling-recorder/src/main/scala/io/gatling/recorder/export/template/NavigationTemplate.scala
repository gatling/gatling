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
import io.gatling.recorder.model.RequestModel
import io.gatling.recorder.model.PauseModel

object NavigationTemplate {

  def render(model: SimulationModel): Seq[(String, String)] = {

      def renderNavigations = {

        val navigations = model.getNavigations.map {
          navigation =>
            {

              val int_name = navigation._2.name

              val requests = navigation._2.requestList.map {

                case requestTuple =>
                  {
                    requestTuple._2 match {
                      case request: RequestModel => {
                        val req_name = request.identifier
                        fast"\t\tRequests._$req_name"
                      }
                      case pause: PauseModel => {

                        val duration = pause.duration
                        fast"\t\texec(pause($duration))"
                      }
                      case ignore => { """""" }
                    }
                  }

              }.mkFastring(",\n")

              fast"""\n\n\tval $int_name = exec(\n$requests\t)"""

            }.mkFastring("")

        }.mkFastring("")

        fast"""$navigations"""

      }

    val output = fast"""
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object Navigations {$renderNavigations

}""".toString()

    List((s"${model.name}_navigations", output))
  }
}
