/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.scenario.template

import io.gatling.commons.util.StringHelper.EmptyFastring
import io.gatling.http.util.HttpHelper.OkCodes
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.scenario.{ RequestBodyBytes, RequestBodyParams }
import io.gatling.recorder.scenario.{ RequestElement, ScenarioExporter }

import com.dongxiguo.fastring.Fastring.Implicits._

private[scenario] object RequestTemplate {

  val BuiltInHttpMethods = List("GET", "PUT", "PATCH", "HEAD", "DELETE", "OPTIONS", "POST")

  def headersBlockName(id: Int) = fast"headers_$id"

  def renderRequest(simulationClass: String, request: RequestElement, extractedUri: ExtractedUris)(implicit config: RecorderConfiguration): Fastring = {
      def renderMethod: Fastring =
        if (BuiltInHttpMethods.contains(request.method)) {
          fast"${request.method.toLowerCase}($renderUrl)"
        } else {
          fast"""httpRequest("${request.method}", $renderUrl)"""
        }

      def usesBaseUrl: Boolean =
        request.printedUrl != request.uri

      def renderUrl =
        if (usesBaseUrl) protectWithTripleQuotes(request.printedUrl)
        else extractedUri.renderUri(request.uri)

      def renderHeaders: String = request.filteredHeadersId
        .map { id =>
          s"""
			.headers(${headersBlockName(id)})"""
        }.getOrElse("")

      def renderLongString(value: String) =
        if (value.nonEmpty)
          value.grouped(65535).map(protectWithTripleQuotes).mkFastring(" + ")
        else
          protectWithTripleQuotes("")

      def renderBodyOrParams: Fastring = request.body.map {
        case RequestBodyBytes(_) => fast"""
			.body(RawFileBody("${ScenarioExporter.requestBodyFileName(request)}"))"""
        case RequestBodyParams(params) => params.map {
          case (key, value) => fast"""
			.formParam(${protectWithTripleQuotes(key)}, ${renderLongString(value)})"""
        }.mkFastring
      }.getOrElse(EmptyFastring)

      def renderCredentials: String = request.basicAuthCredentials.map {
        case (username, password) => s"""
			.basicAuth(${protectWithTripleQuotes(username)},${protectWithTripleQuotes(password)})"""
      }.getOrElse("")

      def renderStatusCheck: Fastring =
        if (!OkCodes.contains(request.statusCode))
          fast"""
			.check(status.is(${request.statusCode}))"""
        else
          EmptyFastring

      def renderResponseBodyCheck: Fastring =
        if (request.responseBody.isDefined && config.http.checkResponseBodies)
          fast"""
			.check(bodyBytes.is(RawFileBody("${ScenarioExporter.responseBodyFileName(request)}")))"""
        else
          EmptyFastring

      def renderResources: Fastring =
        if (request.nonEmbeddedResources.nonEmpty)
          fast"""
			.resources(${
            request.nonEmbeddedResources.zipWithIndex.map { case (resource, i) => renderRequest(simulationClass, resource, extractedUri) }.mkString(
              """,
            """.stripMargin
            )
          })"""
        else
          EmptyFastring

    fast"""http("request_${request.id}")
			.$renderMethod$renderHeaders$renderBodyOrParams$renderCredentials$renderResources$renderStatusCheck$renderResponseBodyCheck"""
  }

  def render(simulationClass: String, request: RequestElement, extractedUri: ExtractedUris)(implicit config: RecorderConfiguration): String =
    fast"exec(${renderRequest(simulationClass, request, extractedUri)})".toString
}
