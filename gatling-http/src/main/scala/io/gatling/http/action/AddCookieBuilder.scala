/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action

import java.net.URI

import com.ning.http.client.Cookie

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef

import io.gatling.core.action.SessionHook
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.ProtocolRegistry
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.cookie.CookieHandling.storeCookie

object AddCookieBuilder {

	def defaultDomain(httpProtocol: HttpProtocol) = {
		val baseUrlHost = httpProtocol.baseURL.map(url => URI.create(url).getHost)
		(session: Session) => baseUrlHost.map(_.success).getOrElse("Neither cookie domain nor baseURL".failure)
	}

	val defaultPath: Expression[String] = _ => "/".success
}

class AddCookieBuilder(name: Expression[String], value: Expression[String], domain: Option[Expression[String]], path: Option[Expression[String]], maxAge: Int) extends ActionBuilder {

	def build(next: ActorRef, protocolRegistry: ProtocolRegistry) = {

		val httpProtocol = protocolRegistry.getProtocol(HttpProtocol.default)

		val resolvedDomain = domain.getOrElse(AddCookieBuilder.defaultDomain(httpProtocol))
		val resolvedPath = path.getOrElse(AddCookieBuilder.defaultPath)

		val expression: Expression[Session] = session => for {
			name <- name(session)
			value <- value(session)
			domain <- resolvedDomain(session)
			path <- resolvedPath(session)
			cookie = new Cookie(domain, name, value, path, maxAge, false)
		} yield storeCookie(session, cookie)

		actor(new SessionHook(expression, next))
	}
}
