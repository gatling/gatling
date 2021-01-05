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

import scala.jdk.CollectionConverters._

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.recorder.config.{ FilterStrategy, RecorderConfiguration }
import io.gatling.recorder.scenario.ProtocolDefinition
import io.gatling.recorder.scenario.ProtocolDefinition.BaseHeadersAndProtocolMethods
import io.gatling.recorder.util.HttpUtils

import io.netty.handler.codec.http.HttpHeaderNames

private[scenario] object ProtocolTemplate {

  private val Indent = "\t" * 2

  def render(protocol: ProtocolDefinition)(implicit config: RecorderConfiguration): String = {

    def renderProxy = {

      def renderSslPort(proxyPort: Int) = config.proxy.outgoing.sslPort match {
        case Some(proxySslPort) if proxySslPort != proxyPort => s".httpsPort($proxySslPort)"
        case _                                               => ""
      }

      def renderCredentials = {
        val credentials = for {
          proxyUsername <- config.proxy.outgoing.username
          proxyPassword <- config.proxy.outgoing.password
        } yield s"""$Eol$Indent.credentials(${protectWithTripleQuotes(proxyUsername)},${protectWithTripleQuotes(proxyPassword)})"""
        credentials.getOrElse("")
      }

      val protocol = for {
        proxyHost <- config.proxy.outgoing.host
        proxyPort <- config.proxy.outgoing.port
      } yield s"""$Eol$Indent.proxy(Proxy("$proxyHost", $proxyPort)${renderSslPort(proxyPort)}$renderCredentials)"""

      protocol.getOrElse("")
    }

    def renderFollowRedirect = if (!config.http.followRedirect) s"$Eol$Indent.disableFollowRedirect" else ""

    def renderInferHtmlResources =
      if (config.http.inferHtmlResources) {
        val filtersConfig = config.filters

        def quotedStringList(xs: Seq[String]): String = xs.map(p => "\"\"\"" + p + "\"\"\"").mkString(", ")
        def blackListPatterns = s"BlackList(${quotedStringList(filtersConfig.blackList.patterns)})"
        def whiteListPatterns = s"WhiteList(${quotedStringList(filtersConfig.whiteList.patterns)})"

        val patterns = filtersConfig.filterStrategy match {
          case FilterStrategy.WhiteListFirst => s"$whiteListPatterns, $blackListPatterns"
          case FilterStrategy.BlackListFirst => s"$blackListPatterns, $whiteListPatterns"
          case FilterStrategy.Disabled       => ""
        }

        s"$Eol$Indent.inferHtmlResources($patterns)"
      } else ""

    def renderAutomaticReferer = if (!config.http.automaticReferer) s"$Eol$Indent.disableAutoReferer" else ""

    def renderHeaders = {
      def renderHeader(methodName: String, headerValue: String) = s"""$Eol$Indent.$methodName(${protectWithTripleQuotes(headerValue)})"""
      protocol.headers
        .entries()
        .asScala
        .map { entry =>
          entry.getKey -> entry.getValue
        }
        .sorted
        .flatMap { case (headerName, headerValue) =>
          val properHeaderValue =
            if (headerName.equalsIgnoreCase(HttpHeaderNames.ACCEPT_ENCODING.toString)) {
              HttpUtils.filterSupportedEncodings(headerValue)
            } else {
              headerValue
            }

          Option(BaseHeadersAndProtocolMethods.get(headerName)).map(renderHeader(_, properHeaderValue)).toList
        }
        .mkString
    }

    s"""
		.baseUrl("${protocol.baseUrl}")$renderProxy$renderFollowRedirect$renderInferHtmlResources$renderAutomaticReferer$renderHeaders""".toString
  }
}
