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
import io.gatling.core.util.StringHelper.emptyFastring
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.http.request.RawFileBody
import io.gatling.recorder.model.SimulationModel
import io.gatling.recorder.model.RequestModel
import io.gatling.recorder.model.RequestBodyBytes
import io.gatling.recorder.model.RequestBodyParams

object RequestBodyTemplate {

  def render(model: SimulationModel): Seq[(String, Array[Byte])] = {

      // get bodies to render
      def output = model.getRequests.toList.sortWith(_.identifier < _.identifier).map {

        request =>
          {
            val req: RequestModel = request
            val name = "_" + req.identifier

            val body = req.body.map {
              case RequestBodyBytes(bytes) => { bytes }
              case RequestBodyParams(_)    => {}
            }

            val fileName = s"${model.name}_REQ_BODY_${request.identifier}.txt"

            (fileName, body)

          }
      }.collect { case (a: String, Some(x: Array[Byte])) => (a, x) }

    output
  }

}
