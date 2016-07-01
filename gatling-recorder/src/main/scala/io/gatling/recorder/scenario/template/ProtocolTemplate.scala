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

import io.gatling.commons.util.StringHelper.{ EmptyFastring, Eol }
import io.gatling.http.HeaderNames
import io.gatling.recorder.config.{ FilterStrategy, RecorderConfiguration }
import io.gatling.recorder.scenario.ProtocolDefinition
import io.gatling.recorder.scenario.ProtocolDefinition.BaseHeaders

import com.dongxiguo.fastring.Fastring.Implicits._

private[scenario] object ProtocolTemplate {

  val Indent = "\t" * 2

  def render(protocol: ProtocolDefinition)(implicit config: RecorderConfiguration) = {

      def renderProxy = {

          def renderSslPort = config.proxy.outgoing.sslPort match {
            case Some(proxySslPort) => s".httpsPort($proxySslPort)"
            case _                  => ""
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
        } yield fast"""$Eol$Indent.proxy(Proxy("$proxyHost", $proxyPort)$renderSslPort$renderCredentials)"""

        protocol.getOrElse(EmptyFastring)
      }

      def renderFollowRedirect = if (!config.http.followRedirect) fast"$Eol$Indent.disableFollowRedirect" else fast""

      def renderInferHtmlResources =
        if (config.http.inferHtmlResources) {
          val filtersConfig = config.filters

            def quotedStringList(xs: Seq[String]): String = xs.map(p => "\"\"\"" + p + "\"\"\"").mkString(", ")
            def blackListPatterns = fast"BlackList(${quotedStringList(filtersConfig.blackList.patterns)})"
            def whiteListPatterns = fast"WhiteList(${quotedStringList(filtersConfig.whiteList.patterns)})"

          val patterns = filtersConfig.filterStrategy match {
            case FilterStrategy.WhitelistFirst => fast"$whiteListPatterns, $blackListPatterns"
            case FilterStrategy.BlacklistFirst => fast"$blackListPatterns, $whiteListPatterns"
            case FilterStrategy.Disabled       => EmptyFastring
          }

          fast"$Eol$Indent.inferHtmlResources($patterns)"
        } else fast""

      def renderAutomaticReferer = if (!config.http.automaticReferer) fast"$Eol$Indent.disableAutoReferer" else fast""

      def renderHeaders = {
          def renderHeader(methodName: String, headerValue: String) = fast"""$Eol$Indent.$methodName(${protectWithTripleQuotes(headerValue)})"""
        protocol.headers.toList.sorted
          .filter {
            case (HeaderNames.Connection, value) => value == "close"
            case _                               => true
          }.flatMap {
            case (headerName, headerValue) =>
              val properHeaderValue =
                if (headerName == HeaderNames.AcceptEncoding)
                  headerValue.stripSuffix(", br")
                else
                  headerValue

              BaseHeaders.get(headerName).map(renderHeader(_, properHeaderValue))
          }.mkFastring
      }

    fast"""
		.baseURL("${protocol.baseUrl}")$renderProxy$renderFollowRedirect$renderInferHtmlResources$renderAutomaticReferer$renderHeaders""".toString
  }
}
