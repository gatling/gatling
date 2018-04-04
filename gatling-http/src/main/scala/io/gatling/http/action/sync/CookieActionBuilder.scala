/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.cookie.CookieSupport._

import io.netty.handler.codec.http.cookie.{ Cookie, DefaultCookie }

object CookieActionBuilder {
  private val NoBaseUrlFailure = "Neither cookie domain nor baseURL".failure
  val DefaultPath: String = "/"

  def defaultDomain(httpCaches: HttpCaches): Expression[String] =
    session =>
      httpCaches.baseUrl(session) match {
        case Some(baseUrl) => Uri.create(baseUrl).getHost.success
        case _             => NoBaseUrlFailure
      }
}

case class AddCookieDsl(
    name:   String,
    value:  Expression[String],
    domain: Option[String]     = None,
    path:   Option[String]     = None,
    maxAge: Option[Long]       = None,
    secure: Boolean            = false
) {
  def withDomain(domain: String) = copy(domain = Some(domain))
  def withPath(path: String) = copy(path = Some(path))
  def withMaxAge(maxAge: Int) = copy(maxAge = Some(maxAge))
  def withSecure(secure: Boolean) = copy(secure = secure)
}

object AddCookieBuilder {

  def apply(cookie: AddCookieDsl) =
    new AddCookieBuilder(cookie.name, cookie.value, cookie.domain, cookie.path, cookie.maxAge.getOrElse(Cookie.UNDEFINED_MAX_AGE), cookie.secure)
}

class AddCookieBuilder(name: String, value: Expression[String], domain: Option[String], path: Option[String], maxAge: Long, secure: Boolean) extends HttpActionBuilder with NameGen {

  import CookieActionBuilder._

  def build(ctx: ScenarioContext, next: Action): Action = {

    import ctx._

    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)

    val requestDomain = domain match {
      case None =>
        // no cookie domain defined, we absolutely need one from the baseUrl
        defaultDomain(httpComponents.httpCaches)
      case _ =>
        // use a mock as requestDomain will be ignored in favor of cookie's one
        EmptyStringExpressionSuccess
    }

    val expression: Expression[Session] = session => for {
      value <- value(session)
      resolvedRequestDomain <- requestDomain(session)
    } yield {
      val cookie = new DefaultCookie(name, value)
      domain.foreach(cookie.setDomain)
      path.foreach(cookie.setPath)
      cookie.setSecure(secure)
      storeCookie(session, resolvedRequestDomain, DefaultPath, cookie)
    }

    new SessionHook(expression, genName("addCookie"), coreComponents.statsEngine, next) with ExitableAction
  }
}

case class GetCookieDsl(
    name:   String,
    domain: Option[String] = None,
    path:   Option[String] = None,
    secure: Boolean        = false,
    saveAs: Option[String] = None
) {
  def withDomain(domain: String) = copy(domain = Some(domain))
  def withPath(path: String) = copy(path = Some(path))
  def withSecure(secure: Boolean) = copy(secure = secure)
  def saveAs(key: String) = copy(saveAs = Some(key))
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

    new SessionHook(expression, genName("getCookie"), coreComponents.statsEngine, next) with ExitableAction
  }
}
