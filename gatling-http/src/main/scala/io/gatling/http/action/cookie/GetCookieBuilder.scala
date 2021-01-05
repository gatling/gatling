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
import io.gatling.core.session._
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.cookie.CookieSupport.getCookieValue

object GetCookieBuilder {

  def apply(cookie: GetCookieDsl): GetCookieBuilder =
    new GetCookieBuilder(cookie.name, cookie.domain, cookie.path, cookie.secure, cookie.saveAs)
}

class GetCookieBuilder(name: Expression[String], domain: Option[Expression[String]], path: Option[String], secure: Boolean, saveAs: Option[String])
    extends HttpActionBuilder
    with NameGen {

  import CookieActionBuilder._

  override def build(ctx: ScenarioContext, next: Action): Action = {

    import ctx._
    val httpProtocol = lookUpHttpComponents(protocolComponentsRegistry).httpProtocol

    val nonEmptyDomain = domain.getOrElse(defaultDomain(httpProtocol))
    val resolvedPath = path.getOrElse(DefaultPath)

    val expression: Expression[Session] = session =>
      for {
        resolvedName <- name(session)
        resolvedDomain <- nonEmptyDomain(session)
        cookieValue <- getCookieValue(session, resolvedDomain, resolvedPath, resolvedName, secure)
      } yield session.set(saveAs.getOrElse(resolvedName), cookieValue)

    new SessionHook(expression, genName("getCookie"), coreComponents.statsEngine, coreComponents.clock, next) with ExitableAction
  }
}
