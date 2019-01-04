/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

case class GetCookieDsl(
    name:   String,
    domain: Option[String] = None,
    path:   Option[String] = None,
    secure: Boolean        = false,
    saveAs: Option[String] = None
) {
  def withDomain(domain: String): GetCookieDsl = copy(domain = Some(domain))
  def withPath(path: String): GetCookieDsl = copy(path = Some(path))
  def withSecure(secure: Boolean): GetCookieDsl = copy(secure = secure)
  def saveAs(key: String): GetCookieDsl = copy(saveAs = Some(key))
}

object GetCookieValueBuilder {

  def apply(cookie: GetCookieDsl) =
    new GetCookieValueBuilder(cookie.name, cookie.domain, cookie.path, cookie.secure, cookie.saveAs)
}

class GetCookieValueBuilder(name: String, domain: Option[String], path: Option[String], secure: Boolean, saveAs: Option[String]) extends HttpActionBuilder with NameGen {

  import CookieActionBuilder._

  override def build(ctx: ScenarioContext, next: Action): Action = {

    import ctx._

    val resolvedDomain = domain
      .map(_.expressionSuccess)
      .getOrElse(defaultDomain(lookUpHttpComponents(protocolComponentsRegistry).httpCaches))
    val resolvedPath = path.getOrElse(DefaultPath)
    val resolvedSaveAs = saveAs.getOrElse(name)

    val expression: Expression[Session] = session => for {
      domain <- resolvedDomain(session)
      cookieValue <- getCookieValue(session, domain, resolvedPath, name, secure)
    } yield session.set(resolvedSaveAs, cookieValue)

    new SessionHook(expression, genName("getCookie"), coreComponents.statsEngine, coreComponents.clock, next) with ExitableAction
  }
}
