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

import scala.jdk.CollectionConverters._

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.recorder.config.{ FilterStrategy, RecorderConfiguration }
import io.gatling.recorder.convert.ProtocolDefinition
import io.gatling.recorder.convert.ProtocolDefinition.BaseHeadersAndProtocolMethods
import io.gatling.recorder.util.HttpUtils

import io.netty.handler.codec.http.HttpHeaderNames

private[convert] class ProtocolTemplate(config: RecorderConfiguration) {

  private def renderProxy = {
    def renderSslPort(proxyPort: Int) = config.proxy.outgoing.sslPort match {
      case Some(proxySslPort) if proxySslPort != proxyPort => s".httpsPort($proxySslPort)"
      case _                                               => ""
    }

    def renderCredentials = {
      val credentials = for {
        proxyUsername <- config.proxy.outgoing.username
        proxyPassword <- config.proxy.outgoing.password
      } yield s""".credentials(${protectWithTripleQuotes(proxyUsername)},${protectWithTripleQuotes(proxyPassword)})"""
      credentials.getOrElse("")
    }

    val protocol = for {
      proxyHost <- config.proxy.outgoing.host
      proxyPort <- config.proxy.outgoing.port
    } yield s""".proxy(Proxy("$proxyHost", $proxyPort)${renderSslPort(proxyPort)}$renderCredentials)"""

    protocol.getOrElse("")
  }

  private def renderFollowRedirect = if (config.http.followRedirect) "" else ".disableFollowRedirect"

  private def renderInferHtmlResources =
    if (config.http.inferHtmlResources) {
      val filtersConfig = config.filters

      def quotedStringList(xs: Seq[String]): String = xs.map(p => "\"\"\"" + p + "\"\"\"").mkString(", ")
      def denyListPatterns = s"DenyList(${quotedStringList(filtersConfig.denyList.patterns)})"
      def allowListPatterns = s"AllowList(${quotedStringList(filtersConfig.allowList.patterns)})"

      val patterns = filtersConfig.filterStrategy match {
        case FilterStrategy.AllowListFirst => s"$allowListPatterns, $denyListPatterns"
        case FilterStrategy.DenyListFirst  => s"$denyListPatterns, $allowListPatterns"
        case FilterStrategy.Disabled       => ""
      }

      s".inferHtmlResources($patterns)"
    } else {
      ""
    }

  private def renderAutomaticReferer = if (config.http.automaticReferer) "" else ".disableAutoReferer"

  private def renderHeaders(protocol: ProtocolDefinition) =
    protocol.headers.entries.asScala
      .map(entry => entry.getKey -> entry.getValue)
      .sorted
      .flatMap { case (headerName, headerValue) =>
        val properHeaderValue =
          if (headerName.equalsIgnoreCase(HttpHeaderNames.ACCEPT_ENCODING.toString)) {
            HttpUtils.filterSupportedEncodings(headerValue)
          } else {
            headerValue
          }

        Option(BaseHeadersAndProtocolMethods.get(headerName))
          .map(methodName => s"""
                                |    .$methodName(${protectWithTripleQuotes(properHeaderValue)})""".stripMargin)
          .toList
      }
      .mkString

  def render(protocol: ProtocolDefinition): String =
    s"""http
       |    .baseUrl("${protocol.baseUrl}")
       |    $renderProxy
       |    $renderFollowRedirect
       |    $renderInferHtmlResources
       |    $renderAutomaticReferer
       |    ${renderHeaders(protocol)}""".stripMargin.linesIterator
      .filter(_.exists(_ != ' '))
      .mkString(Eol)
}
