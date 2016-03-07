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
package io.gatling.http.action.sync

import io.gatling.core.action.{ Action, ExitableAction, SessionHook }
import io.gatling.core.session._
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.cookie.CookieJar
import io.gatling.http.cookie.CookieSupport.storeCookie
import io.gatling.http.protocol.HttpProtocol

import org.asynchttpclient.cookie.Cookie
import org.asynchttpclient.uri.Uri

case class CookieDSL(name: Expression[String], value: Expression[String],
                     domain: Option[Expression[String]] = None,
                     path:   Option[Expression[String]] = None,
                     maxAge: Option[Long]               = None) {

  def withDomain(domain: Expression[String]) = copy(domain = Some(domain))
  def withPath(path: Expression[String]) = copy(path = Some(path))
  def withMaxAge(maxAge: Int) = copy(maxAge = Some(maxAge))
}

object AddCookieBuilder {

  val NoBaseUrlFailure = "Neither cookie domain nor baseURL".expressionFailure
  val DefaultPath = "/".expressionSuccess

  def apply(cookie: CookieDSL) =
    new AddCookieBuilder(cookie.name, cookie.value, cookie.domain, cookie.path, cookie.maxAge.getOrElse(CookieJar.UnspecifiedMaxAge))
}

class AddCookieBuilder(name: Expression[String], value: Expression[String], domain: Option[Expression[String]], path: Option[Expression[String]], maxAge: Long) extends HttpActionBuilder with NameGen {

  import AddCookieBuilder._

  private def defaultDomain(httpProtocol: HttpProtocol) =
    httpProtocol.baseUrlIterator match {
      case Some(it) => Uri.create(it.next()).getHost.expressionSuccess
      case _        => NoBaseUrlFailure
    }

  def build(ctx: ScenarioContext, next: Action): Action = {

    import ctx._

    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val resolvedDomain = domain.getOrElse(defaultDomain(httpComponents.httpProtocol))
    val resolvedPath = path.getOrElse(DefaultPath)

    val expression: Expression[Session] = session => for {
      name <- name(session)
      value <- value(session)
      domain <- resolvedDomain(session)
      path <- resolvedPath(session)
    } yield {
      val cookie = new Cookie(name, value, false, domain, path, maxAge, false, false)
      storeCookie(session, domain, path, cookie)
    }

    new SessionHook(expression, genName("addCookie"), coreComponents.statsEngine, next) with ExitableAction
  }
}
