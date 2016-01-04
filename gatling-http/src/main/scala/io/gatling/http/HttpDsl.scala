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
package io.gatling.http

import io.gatling.commons.stats.KO
import io.gatling.core.body.{ RawFileBodies, ElFileBodies }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.action.sync._
import io.gatling.http.check.HttpCheckSupport
import io.gatling.http.check.async.AsyncCheckSupport
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.feeder.SitemapFeederSupport
import io.gatling.http.protocol.{ HttpProtocolBuilder, HttpProxyBuilder }
import io.gatling.http.request.{ BodyPart, ExtraInfo }
import io.gatling.http.request.builder.Http
import io.gatling.http.request.builder.polling.Polling
import io.gatling.http.request.builder.sse.Sse
import io.gatling.http.request.builder.ws.Ws

trait HttpDsl extends HttpCheckSupport with AsyncCheckSupport with SitemapFeederSupport {

  def http(implicit configuration: GatlingConfiguration) = HttpProtocolBuilder(configuration)

  val Proxy = HttpProxyBuilder.apply _

  def http(requestName: Expression[String]) = new Http(requestName)
  def addCookie(cookie: CookieDSL) = AddCookieBuilder(cookie)
  def flushSessionCookies = CookieSupport.FlushSessionCookies
  def flushCookieJar = CookieSupport.FlushCookieJar
  def flushHttpCache = new FlushCacheBuilder

  def sse(requestName: Expression[String]) = new Sse(requestName)
  def sse(requestName: Expression[String], sseName: String) = new Sse(requestName, sseName)
  def ws(requestName: Expression[String]) = new Ws(requestName)
  def ws(requestName: Expression[String], wsName: String) = new Ws(requestName, wsName)
  def polling = new Polling()

  val HttpHeaderNames = HeaderNames
  val HttpHeaderValues = HeaderValues

  val dumpSessionOnFailure: ExtraInfo => List[Any] = extraInfo => extraInfo.status match {
    case KO => List(extraInfo.session)
    case _  => Nil
  }

  def Cookie = CookieDSL

  def ElFileBodyPart(filePath: Expression[String])(implicit configuration: GatlingConfiguration, elFileBodies: ElFileBodies): BodyPart =
    BodyPart.elFileBodyPart(None, filePath)
  def ElFileBodyPart(name: Expression[String], filePath: Expression[String])(implicit configuration: GatlingConfiguration, elFileBodies: ElFileBodies): BodyPart =
    BodyPart.elFileBodyPart(Some(name), filePath)

  def StringBodyPart(string: Expression[String])(implicit configuration: GatlingConfiguration): BodyPart =
    BodyPart.stringBodyPart(None, string)
  def StringBodyPart(name: Expression[String], string: Expression[String])(implicit configuration: GatlingConfiguration): BodyPart =
    BodyPart.stringBodyPart(Some(name), string)

  def RawFileBodyPart(filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): BodyPart =
    BodyPart.rawFileBodyPart(None, filePath)
  def RawFileBodyPart(name: Expression[String], filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): BodyPart =
    BodyPart.rawFileBodyPart(Some(name), filePath)

  def ByteArrayBodyPart(bytes: Expression[Array[Byte]]): BodyPart = BodyPart.byteArrayBodyPart(None, bytes)
  def ByteArrayBodyPart(name: Expression[String], bytes: Expression[Array[Byte]]): BodyPart = BodyPart.byteArrayBodyPart(Some(name), bytes)
}
