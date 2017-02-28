/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.sync

import io.gatling.commons.validation._
import io.gatling.core.action.{ Action, ExitableAction, SessionHook }
import io.gatling.core.session._
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.cache.HttpCaches
import io.gatling.http.cookie.CookieJar
import io.gatling.http.cookie.CookieSupport.storeCookie

import io.netty.handler.codec.http.cookie.DefaultCookie
import org.asynchttpclient.uri.Uri

case class AddCookieDsl(
    name:   String,
    value:  Expression[String],
    domain: Option[String]     = None,
    path:   Option[String]     = None,
    maxAge: Option[Long]       = None
) {

  def withDomain(domain: String) = copy(domain = Some(domain))
  def withPath(path: String) = copy(path = Some(path))
  def withMaxAge(maxAge: Int) = copy(maxAge = Some(maxAge))
}

object AddCookieBuilder {

  val NoBaseUrlFailure = "Neither cookie domain nor baseURL".failure
  val DefaultPath = "/"

  def apply(cookie: AddCookieDsl) =
    new AddCookieBuilder(cookie.name, cookie.value, cookie.domain, cookie.path, cookie.maxAge.getOrElse(CookieJar.UnspecifiedMaxAge))
}

class AddCookieBuilder(name: String, value: Expression[String], domain: Option[String], path: Option[String], maxAge: Long) extends HttpActionBuilder with NameGen {

  import AddCookieBuilder._

  private def defaultDomain(httpCaches: HttpCaches): Expression[String] =
    session => {
      httpCaches.baseUrl(session) match {
        case Some(baseUrl) => Uri.create(baseUrl).getHost.success
        case _             => NoBaseUrlFailure
      }
    }

  def build(ctx: ScenarioContext, next: Action): Action = {

    import ctx._

    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val resolvedDomain = domain.map(_.expressionSuccess).getOrElse(defaultDomain(httpComponents.httpCaches))
    val resolvedPath = path.getOrElse(DefaultPath)

    val expression: Expression[Session] = session => for {
      value <- value(session)
      domain <- resolvedDomain(session)
    } yield storeCookie(session, domain, resolvedPath, new DefaultCookie(name, value))

    new SessionHook(expression, genName("addCookie"), coreComponents.statsEngine, next) with ExitableAction
  }
}
