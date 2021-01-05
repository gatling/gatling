/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.util.Locale

import io.gatling.http.util.HttpHelper
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.scenario.{ RequestBodyBytes, RequestBodyParams }
import io.gatling.recorder.scenario.{ RequestElement, ScenarioExporter }

private[scenario] object RequestTemplate {

  val BuiltInHttpMethods = List("GET", "PUT", "PATCH", "HEAD", "DELETE", "OPTIONS", "POST")
  val MaxLiteralSize = 65534

  def headersBlockName(id: Int) = s"headers_$id"

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def renderRequest(simulationClass: String, request: RequestElement, extractedUri: ExtractedUris)(implicit config: RecorderConfiguration): String = {
    def renderMethod: String =
      if (BuiltInHttpMethods.contains(request.method)) {
        s"${request.method.toLowerCase(Locale.ROOT)}($renderUrl)"
      } else {
        s"""httpRequest("${request.method}", $renderUrl)"""
      }

    def usesBaseUrl: Boolean =
      request.printedUrl != request.uri

    def renderUrl =
      if (usesBaseUrl) protectWithTripleQuotes(request.printedUrl)
      else extractedUri.renderUri(request.uri)

    def renderHeaders: String =
      request.filteredHeadersId
        .map { id =>
          s"""
			.headers(${headersBlockName(id)})"""
        }
        .getOrElse("")

    def renderLongString(value: String) =
      if (value.length > MaxLiteralSize)
        s"""Seq(${value.grouped(MaxLiteralSize).map(protectWithTripleQuotes).mkString(", ")}).mkString"""
      else
        protectWithTripleQuotes(value)

    def renderBodyOrParams: String =
      request.body
        .map {
          case _: RequestBodyBytes => s"""
			.body(RawFileBody("${ScenarioExporter.requestBodyRelativeFilePath(request)}"))"""
          case RequestBodyParams(params) =>
            params.map { case (key, value) =>
              s"""
			.formParam(${protectWithTripleQuotes(key)}, ${renderLongString(value)})"""
            }.mkString
        }
        .getOrElse("")

    def renderCredentials: String =
      request.basicAuthCredentials
        .map { case (username, password) =>
          s"""
			.basicAuth(${protectWithTripleQuotes(username)},${protectWithTripleQuotes(password)})"""
        }
        .getOrElse("")

    def renderStatusCheck: String =
      if (!HttpHelper.isOk(request.statusCode))
        s"""
			.check(status.is(${request.statusCode}))"""
      else
        ""

    def renderResponseBodyCheck: String =
      if (request.responseBody.isDefined && config.http.checkResponseBodies)
        s"""
			.check(bodyBytes.is(RawFileBody("${ScenarioExporter.responseBodyRelativeFilePath(request)}")))"""
      else
        ""

    def renderResources: String =
      if (request.nonEmbeddedResources.nonEmpty)
        s"""
			.resources(${request.nonEmbeddedResources.zipWithIndex
          .map { case (resource, _) => renderRequest(simulationClass, resource, extractedUri) }
          .mkString(
            """,
            """.stripMargin
          )})"""
      else
        ""
    val prefix = if (config.http.useSimulationAsPrefix) simulationClass else "request"
    val postfix = if (config.http.useMethodAndUriAsPostfix) ":" + sanitizeRequestPostfix(s"${request.method}_${request.uri}") else ""
    s"""http("${prefix}_${request.id}${postfix}")
			.$renderMethod$renderHeaders$renderBodyOrParams$renderCredentials$renderResources$renderStatusCheck$renderResponseBodyCheck"""
  }

  def sanitizeRequestPostfix(postfix: String): String = postfix.replaceAll("[^-._=:/?&A-Za-z0-9]", "_")

  def render(simulationClass: String, request: RequestElement, extractedUri: ExtractedUris)(implicit config: RecorderConfiguration): String =
    s"exec(${renderRequest(simulationClass, request, extractedUri)})".toString
}
