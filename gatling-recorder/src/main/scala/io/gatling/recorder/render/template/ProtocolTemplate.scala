/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.render.template

import scala.jdk.CollectionConverters._

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.render.ProtocolDefinition
import io.gatling.recorder.render.ProtocolDefinition.BaseHeadersAndProtocolMethods
import io.gatling.recorder.util.HttpUtils

import io.netty.handler.codec.http.HttpHeaderNames

private[render] class ProtocolTemplate(config: RecorderConfiguration) {
  private val format = config.core.format

  private def renderProxy = {
    def renderSslPort(proxyPort: Int) = config.proxy.outgoing.sslPort match {
      case Some(proxySslPort) if proxySslPort != proxyPort => s".httpsPort($proxySslPort)"
      case _                                               => ""
    }

    def renderCredentials = {
      val credentials = for {
        proxyUsername <- config.proxy.outgoing.username
        proxyPassword <- config.proxy.outgoing.password
      } yield s""".credentials(${proxyUsername.protect(format)},${proxyPassword.protect(format)})"""
      credentials.getOrElse("")
    }

    val protocol = for {
      proxyHost <- config.proxy.outgoing.host
      proxyPort <- config.proxy.outgoing.port
    } yield s""".proxy(Proxy("$proxyHost", $proxyPort)${renderSslPort(proxyPort)}$renderCredentials)"""

    protocol.getOrElse("")
  }

  private def renderFollowRedirect =
    if (config.http.followRedirect) {
      ""
    } else {
      s".disableFollowRedirect${format.parameterlessMethodCall}"
    }

  private def renderInferHtmlResources =
    if (config.http.inferHtmlResources) {
      val filtersConfig = config.filters

      def quotedStringList(xs: Seq[String]): String =
        xs.map(_.protect(format)).mkString(", ")

      val patterns =
        if (filtersConfig.enabled) {
          val allowList = if (filtersConfig.denyList.patterns.nonEmpty) s"AllowList(${quotedStringList(filtersConfig.allowList.patterns)})" else ""
          val denyList = if (filtersConfig.denyList.patterns.nonEmpty) s"DenyList(${quotedStringList(filtersConfig.denyList.patterns)})" else ""
          List(allowList, denyList).filter(_.nonEmpty).mkString(", ")
        } else {
          ""
        }

      s".inferHtmlResources($patterns)"
    } else {
      ""
    }

  private def renderAutomaticReferer =
    if (config.http.automaticReferer) {
      ""
    } else {
      s".disableAutoReferer${format.parameterlessMethodCall}"
    }

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
          .map(methodName => s""".$methodName(${properHeaderValue.protect(format)})""".stripMargin)
          .toList
      }
      .mkString(Eol)

  def render(protocol: ProtocolDefinition): String = {
    val protocolType = format match {
      case RenderingFormat.Scala | RenderingFormat.Kotlin          => "private val"
      case RenderingFormat.Java11 | RenderingFormat.Java17         => "private HttpProtocolBuilder"
      case RenderingFormat.JavaScript | RenderingFormat.TypeScript => "const"
    }

    s"""$protocolType httpProtocol = http
       |  .baseUrl("${protocol.baseUrl}")
       |${renderProxy.indent(2)}
       |${renderFollowRedirect.indent(2)}
       |${renderInferHtmlResources.indent(2)}
       |${renderAutomaticReferer.indent(2)}
       |${renderHeaders(protocol).indent(2)}${format.lineTermination}""".stripMargin.noEmptyLines
  }
}
