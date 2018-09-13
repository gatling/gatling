/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import io.gatling.core.body.{ ElFileBodies, RawFileBodies }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.action.cache.FlushCacheBuilder
import io.gatling.http.action.cookie.{ AddCookieBuilder, AddCookieDsl, GetCookieDsl, GetCookieValueBuilder }
import io.gatling.http.action.sse.Sse
import io.gatling.http.action.ws.Ws
import io.gatling.http.check.HttpCheckSupport
import io.gatling.http.check.sse.SseCheckSupport
import io.gatling.http.check.ws.WsCheckSupport
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.feeder.SitemapFeederSupport
import io.gatling.http.protocol.{ HttpProtocolBuilder, ProxyBuilder }
import io.gatling.http.request.BodyPart
import io.gatling.http.request.builder.Http
import io.gatling.http.request.builder.polling.Polling

trait HttpDsl extends HttpCheckSupport with WsCheckSupport with SseCheckSupport with SitemapFeederSupport {

  def http(implicit configuration: GatlingConfiguration) = HttpProtocolBuilder(configuration)

  def Proxy(host: String, port: Int) = ProxyBuilder(host, port)

  def http(requestName: Expression[String]) = new Http(requestName)
  def addCookie(cookie: AddCookieDsl) = AddCookieBuilder(cookie)
  def getCookieValue(cookie: GetCookieDsl) = GetCookieValueBuilder(cookie)
  def flushSessionCookies = CookieSupport.FlushSessionCookies
  def flushCookieJar = CookieSupport.FlushCookieJar
  def flushHttpCache = new FlushCacheBuilder

  val sse = Sse
  val ws = Ws
  def polling = new Polling()

  val HttpHeaderNames = HeaderNames
  val HttpHeaderValues = HeaderValues

  def Cookie = AddCookieDsl
  def CookieKey = GetCookieDsl

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
