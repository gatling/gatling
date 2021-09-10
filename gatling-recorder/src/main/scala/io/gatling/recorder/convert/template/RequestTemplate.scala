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

package io.gatling.recorder.convert.template

import java.util.Locale

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.http.util.HttpHelper
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.convert.{ DumpedBody, RequestBodyParams, RequestElement }

private[convert] object RequestTemplate {
  def sanitizeRequestPostfix(postfix: String): String = postfix.replaceAll("[^-._=:/?&A-Za-z0-9]", "_")

  private val BuiltInHttpMethods = List("GET", "PUT", "PATCH", "HEAD", "DELETE", "OPTIONS", "POST")
  private val MaxLiteralSize = 65534
}

private[convert] class RequestTemplate(
    requestBodies: Map[Int, DumpedBody],
    responseBodies: Map[Int, DumpedBody],
    configuration: RecorderConfiguration
) {
  private def renderMethod(request: RequestElement, extractedUri: ExtractedUris): String = {
    val renderedUrl =
      if (request.printedUrl == request.uri) {
        extractedUri.renderUri(request.uri)
      } else {
        protectWithTripleQuotes(request.printedUrl)
      }

    if (RequestTemplate.BuiltInHttpMethods.contains(request.method)) {
      s"${request.method.toLowerCase(Locale.ROOT)}($renderedUrl)"
    } else {
      s"""httpRequest("${request.method}", $renderedUrl)"""
    }
  }

  private def renderHeaders(request: RequestElement): String =
    request.filteredHeadersId match {
      case Some(id) => s""".headers(${SimulationTemplate.headersBlockName(id)})"""
      case _        => ""
    }

  private def renderBodyOrParams(request: RequestElement): String = {

    def renderLongString(value: String) =
      if (value.length > RequestTemplate.MaxLiteralSize)
        s"""Seq(${value.grouped(RequestTemplate.MaxLiteralSize).map(protectWithTripleQuotes).mkString(", ")}).mkString"""
      else
        protectWithTripleQuotes(value)

    request.body match {
      case Some(RequestBodyParams(params)) =>
        params.map { case (key, value) =>
          s"""
             |			.formParam(${protectWithTripleQuotes(key)}, ${renderLongString(value)})""".stripMargin
        }.mkString
      case _ =>
        requestBodies.get(request.id) match {
          case Some(dumpedBody) => s""".body(RawFileBody("${dumpedBody.classPathLocation}"))"""
          case _                => ""
        }
    }
  }

  private def renderBasicAuthCredentials(request: RequestElement): String =
    request.basicAuthCredentials match {
      case Some((username, password)) =>
        s""".basicAuth(${protectWithTripleQuotes(username)},${protectWithTripleQuotes(password)})"""
      case _ =>
        ""
    }

  private def renderResources(simulationClass: String, request: RequestElement, extractedUri: ExtractedUris): String =
    if (request.nonEmbeddedResources.nonEmpty) {
      s""".resources(
         |        ${request.nonEmbeddedResources.zipWithIndex
        .map { case (resource, _) => render(simulationClass, resource, extractedUri, mainRequest = false) }
        .mkString(
          """,
            |        """.stripMargin
        )})""".stripMargin
    } else {
      ""
    }

  private def renderStatusCheck(request: RequestElement): String =
    if (!HttpHelper.isOk(request.statusCode)) {
      s".check(status.is(${request.statusCode}))"
    } else {
      ""
    }

  private def renderResponseBodyCheck(request: RequestElement): String =
    responseBodies.get(request.id) match {
      case Some(dumpedBody) =>
        s""".check(bodyBytes.is(RawFileBody("${dumpedBody.classPathLocation}")))"""
      case _ =>
        ""
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def render(simulationClass: String, request: RequestElement, extractedUri: ExtractedUris, mainRequest: Boolean): String = {
    val leftMargin = " " * (if (mainRequest) 6 else 10)
    val prefix = if (configuration.http.useSimulationAsPrefix) simulationClass else "request"
    val postfix = if (configuration.http.useMethodAndUriAsPostfix) ":" + RequestTemplate.sanitizeRequestPostfix(s"${request.method}_${request.uri}") else ""
    s"""http("${prefix}_${request.id}$postfix")
       |$leftMargin.${renderMethod(request, extractedUri)}
       |$leftMargin${renderHeaders(request)}
       |$leftMargin${renderBodyOrParams(request)}
       |$leftMargin${renderBasicAuthCredentials(request)}
       |$leftMargin${renderResources(simulationClass, request, extractedUri)}
       |$leftMargin${renderStatusCheck(request)}
       |$leftMargin${renderResponseBodyCheck(request)}""".stripMargin.linesIterator
      .filter(_.exists(_ != ' '))
      .mkString(Eol)
  }
}
