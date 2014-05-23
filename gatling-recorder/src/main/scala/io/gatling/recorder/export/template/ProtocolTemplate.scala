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

import io.gatling.recorder.enumeration.FilterStrategy._
import io.gatling.core.util.StringHelper.emptyFastring
import io.gatling.core.util.StringHelper.eol
import io.gatling.recorder.config.RecorderConfiguration
import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.recorder.model.ProtocolModel
import io.gatling.http.HeaderNames
import scala.annotation.tailrec
import io.gatling.recorder.model.SimulationModel

object ProtocolTemplate {

  val T = "\t" * 2
  val N = "\n"

  def render(model: SimulationModel): Seq[(String, String)] = {

    val protocol = renderProtocol(model.getProtocol)
    val headers = renderHeaders(model)

    val output = fast"""package ${model.packageName}

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Protocol {
    
\tval httpProtocol = http$protocol$headers

}""".toString

    List((s"${model.name}_protocol", output))
  }

  /**
   * protocol only
   */
  def renderProtocol(protocol: ProtocolModel): String = {

      def renderProxy = {

          def renderSslPort = protocol.proxy_outgoing_port.map(proxySslPort => fast""".httpsPort($proxySslPort)""").getOrElse(emptyFastring)

          def renderCredentials = {
            val credentials1 = protocol.proxyCredentials match {
              case Some(s) => {
                fast"""$T.credentials(${protectWithTripleQuotes(s.split("|")(0).toString)},${protectWithTripleQuotes(s.split("|")(0).toString)})$N"""
              }
              case _ => emptyFastring
            }
            credentials1.mkString("""""")
          }

        val proxyProtocol = for {
          proxyHost <- protocol.proxy_outgoing_host
          proxyPort <- protocol.proxy_outgoing_port
        } yield fast"""$T.proxy(Proxy("$proxyHost", $proxyPort)$renderSslPort$renderCredentials)"""

        proxyProtocol.getOrElse(emptyFastring)
      }

      def renderFollowRedirect = if (!protocol.http_followRedirect) fast"$T.disableFollowRedirect" else emptyFastring

      // TODO - needs integrating and testing
      def renderFetchHtmlResources = if (protocol.http_fetchHtmlResources) {
        val filtersConfig = protocol.domain_filters

          def quotedStringList(xs: Seq[String]): String = xs.map(p => "\"\"\"" + p + "\"\"\"").mkString(", ")
          def backListPatterns = fast"black = BlackList(${quotedStringList(filtersConfig.blackList.patterns)})"
          def whiteListPatterns = fast"white = WhiteList(${quotedStringList(filtersConfig.whiteList.patterns)})"

        val patterns = filtersConfig.filterStrategy match {
          case WHITELIST_FIRST => fast"$whiteListPatterns, $backListPatterns"
          case BLACKLIST_FIRST => fast"$backListPatterns, $whiteListPatterns"
          case DISABLED        => emptyFastring
        }

        fast"""$T.fetchHtmlResources($patterns)"""
      } else emptyFastring

      def renderAutomaticReferer = if (!protocol.http_automaticReferer) fast"""$T.disableAutoReferer""" else emptyFastring

      def renderProtocolGlobalHeaders = {
          def renderHeader(methodName: String, headerValue: String): Fastring = {
            fast"""$T.$methodName(\"\"\"$headerValue\"\"\")$N"""
          }

        protocol.baseHeaders.map {
          header => renderHeader(header._1, header._2)
        }.mkFastring
      }

    fast"""$N$N$T.baseURL("${protocol.baseUrl}")
$renderProxy
$renderFollowRedirect
$renderFetchHtmlResources
$renderAutomaticReferer
$renderProtocolGlobalHeaders""".toString
  }

  /**
   * headers only
   */
  def renderHeaders(model: SimulationModel): String = {

      def renderHeaders = {

          def printHeaders(headers: Seq[(String, String)]) = {
            if (headers.size > 1) {
              val mapContent = headers.map {
                case (name, value) => fast"$T$T${protectWithTripleQuotes(name)} -> ${protectWithTripleQuotes(value)}"
              }.mkFastring(",\n")
              fast"""Map($N$mapContent$N$T)"""
            } else {
              val (name, value) = headers(0)
              fast"Map(${protectWithTripleQuotes(name)} -> ${protectWithTripleQuotes(value)})"
            }
          }

        model.getProtocol.headers
          .map { case (headersBlockIdentifier, headersBlock) => fast"""\tval headers_${headersBlockIdentifier} = ${printHeaders(headersBlock)}""" }
          .mkFastring("\n\n")
      }

    fast"""$N$renderHeaders""".toString
  }

}

