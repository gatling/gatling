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
import io.gatling.recorder.model.SimulationModel
import io.gatling.recorder.model.RequestModel
import io.gatling.recorder.model.RequestBodyBytes
import io.gatling.recorder.model.RequestBodyParams

object RequestTemplate {

  def render(model: SimulationModel): Seq[(String, String)] = {

    val builtInHttpMethods = List("GET", "PUT", "PATCH", "HEAD", "DELETE", "OPTIONS", "POST")

      def renderRequest(request: RequestModel) = {

          def renderMethod =
            if (builtInHttpMethods.contains(request.method)) {
              fast"${request.method.toLowerCase}($renderUrl)"
            } else {
              fast"""httpRequest("$request.method", Left($renderUrl))"""
            }

          def renderUrl = protectWithTripleQuotes(request.printedUrl)

          def renderHeaders = request.header_identifier //filteredHeadersId
            .map { id =>
              s"""\n\t\t.headers(Protocol.headers_${id})"""
            }.getOrElse("")

          def renderBodyOrParams = request.body.map {
            case RequestBodyBytes(_) => fast"""\n\t\t.body(RawFileBody("${model.name}_request_${request.identifier}.txt"))"""
            case RequestBodyParams(params) => params.map {
              case (key, value) => fast"""\n\t\t.param(${protectWithTripleQuotes(key)}, ${protectWithTripleQuotes(value)})"""
            }.mkFastring
          }.getOrElse(emptyFastring)

          def renderCredentials = request.basicAuthCredentials.map {
            case (username, password) => s"""\n\t\t.basicAuth(${protectWithTripleQuotes(username)},${protectWithTripleQuotes(password)})"""
          }.getOrElse("")

          def renderStatusCheck =
            fast"""\n\n\t\t.check(status.is(${request.statusCode}))"""

        fast"""\n\t\thttp("${request.identifier}")\n\t\t.$renderMethod$renderHeaders$renderBodyOrParams$renderCredentials$renderStatusCheck""".toString
      }

      def renderRequests = {
        
          def n = model.getRequests.toList.sortWith(_.identifier < _.identifier).map {

            request =>
              {
                val req: RequestModel = request
                val name = "_" + req.identifier

                fast"""\n\n\tval $name = exec(${renderRequest(req)}\n\t\t)"""

              }
          }.mkFastring

        fast"""$n"""
      }

    val output = fast"""
import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Requests {$renderRequests
}""".toString()

    List((s"${model.name}_requests", output))
  }
}
