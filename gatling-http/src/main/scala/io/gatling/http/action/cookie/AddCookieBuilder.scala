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

package io.gatling.http.action.cookie

import io.gatling.core.action.{ Action, ExitableAction, SessionHook }
import io.gatling.core.session.{ EmptyStringExpressionSuccess, Expression, Session }
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.cookie.CookieSupport.storeCookie

import io.netty.handler.codec.http.cookie.{ Cookie, DefaultCookie }

object AddCookieBuilder {

  def apply(cookie: AddCookieDsl): AddCookieBuilder =
    new AddCookieBuilder(cookie.name, cookie.value, cookie.domain, cookie.path, cookie.maxAge.getOrElse(Cookie.UNDEFINED_MAX_AGE), cookie.secure)
}

class AddCookieBuilder(name: Expression[String], value: Expression[String], domain: Option[String], path: Option[String], maxAge: Long, secure: Boolean)
    extends HttpActionBuilder
    with NameGen {

  import CookieActionBuilder._

  def build(ctx: ScenarioContext, next: Action): Action = {

    import ctx._

    val clock = ctx.coreComponents.clock
    val httpProtocol = lookUpHttpComponents(protocolComponentsRegistry).httpProtocol

    val requestDomain = domain match {
      case None =>
        // no cookie domain defined, we absolutely need one from the baseUrl
        defaultDomain(httpProtocol)
      case _ =>
        // use a mock as requestDomain will be ignored in favor of cookie's one
        EmptyStringExpressionSuccess
    }

    val expression: Expression[Session] = session =>
      for {
        resolvedName <- name(session)
        resoledValue <- value(session)
        resolvedRequestDomain <- requestDomain(session)
      } yield {
        val cookie = new DefaultCookie(resolvedName, resoledValue)
        domain.foreach(cookie.setDomain)
        path.foreach(cookie.setPath)
        cookie.setSecure(secure)
        storeCookie(session, resolvedRequestDomain, DefaultPath, cookie, clock.nowMillis)
      }

    new SessionHook(expression, genName("addCookie"), coreComponents.statsEngine, coreComponents.clock, next) with ExitableAction
  }
}
