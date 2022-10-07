/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.http.cache

import java.net.InetAddress

import scala.util.control.NonFatal

import io.gatling.commons.util.CircularIterator
import io.gatling.commons.validation._
import io.gatling.core.session._
import io.gatling.http.client.uri.Uri
import io.gatling.http.protocol.HttpProtocol

import com.typesafe.scalalogging.LazyLogging

private[http] object BaseUrlSupport extends LazyLogging {
  private val BaseUrlAttributeName: String = SessionPrivateAttributes.generatePrivateAttribute("http.cache.baseUrl")

  private val WsBaseUrlAttributeName: String = SessionPrivateAttributes.generatePrivateAttribute("http.cache.wsBaseUrl")

  private def preResolve(baseUrl: String, aliasedHostnames: Set[String]): Unit =
    try {
      val uri = Uri.create(baseUrl)
      if (!aliasedHostnames.contains(uri.getHost)) {
        InetAddress.getAllByName(uri.getHost)
      }
    } catch {
      case NonFatal(e) =>
        logger.debug(s"Couldn't pre-resolve hostname from baseUrl $baseUrl", e)
    }

  private def setBaseUrl(baseUrls: List[String], attributeName: String, aliasedHostnames: Set[String]): Session => Session = {
    baseUrls.foreach(preResolve(_, aliasedHostnames))

    baseUrls match {
      case Nil        => Session.Identity
      case url :: Nil => _.set(attributeName, url)
      case urls =>
        val it = CircularIterator(urls.toVector, threadSafe = true)
        _.set(attributeName, it.next())
    }
  }

  def setHttpBaseUrl(httpProtocol: HttpProtocol): Session => Session =
    setBaseUrl(httpProtocol.baseUrls, BaseUrlAttributeName, httpProtocol.dnsPart.hostNameAliases.keySet)

  def setWsBaseUrl(httpProtocol: HttpProtocol): Session => Session =
    setBaseUrl(httpProtocol.wsPart.wsBaseUrls, WsBaseUrlAttributeName, httpProtocol.dnsPart.hostNameAliases.keySet)

  private def baseUrl(session: Session, attributeName: String): Option[String] =
    session.attributes.get(attributeName).map(_.asInstanceOf[String])

  def httpBaseUrl(httpProtocol: HttpProtocol): Session => Option[String] =
    httpProtocol.baseUrls match {
      case Nil => _ => None
      case single :: Nil =>
        val s = Some(single)
        _ => s
      case _ => baseUrl(_, BaseUrlAttributeName)
    }

  def wsBaseUrl(httpProtocol: HttpProtocol): Session => Option[String] =
    httpProtocol.wsPart.wsBaseUrls match {
      case Nil => _ => None
      case single :: Nil =>
        val s = Some(single)
        _ => s
      case _ => baseUrl(_, WsBaseUrlAttributeName)
    }

  def defaultDomain(httpProtocol: HttpProtocol, failure: Expression[String]): Expression[String] =
    httpProtocol.baseUrls match {
      case Nil =>
        httpProtocol.wsPart.wsBaseUrls match {
          case Nil           => failure
          case single :: Nil => Uri.create(single).getHost.expressionSuccess
          case _             => session => Uri.create(baseUrl(session, WsBaseUrlAttributeName).get).getHost.success
        }

      case single :: Nil => Uri.create(single).getHost.expressionSuccess
      case _             => session => Uri.create(baseUrl(session, BaseUrlAttributeName).get).getHost.success
    }
}
