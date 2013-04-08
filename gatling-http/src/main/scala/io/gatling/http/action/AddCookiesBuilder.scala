/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import akka.actor.{ ActorRef, Props }
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.system
import io.gatling.core.config.ProtocolConfigurationRegistry
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.ValidationList
import com.ning.http.client.{ Cookie => AHCCookie }

case class Cookie(domain: Expression[String], name: Expression[String], value: Expression[String], path: Expression[String])

object AddCookiesBuilder {

	def apply(url: Expression[String], cookies: List[Cookie]) = {

		val cookiesExpression: Expression[List[AHCCookie]] = (session: Session) =>
			cookies.map { cookie =>
				for {
					domain <- cookie.domain(session)
					name <- cookie.name(session)
					value <- cookie.value(session)
					path <- cookie.path(session)

				} yield new AHCCookie(domain, name, value, path, 100000, false)
			}.sequence

		new AddCookiesBuilder(url, cookiesExpression)
	}
}

class AddCookiesBuilder(url: Expression[String], cookies: Expression[List[AHCCookie]]) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new AddCookies(url, cookies, next)))
}