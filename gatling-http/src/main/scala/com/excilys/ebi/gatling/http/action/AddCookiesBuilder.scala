/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.{ Expression, Session }
import com.ning.http.client.{ Cookie => AHCCookie }

import akka.actor.{ ActorRef, Props }
import scalaz.Validation
import scalaz.Scalaz.{ ToTraverseOps, listInstance, stringInstance }

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
			}.sequence[({ type l[a] = Validation[String, a] })#l, AHCCookie]

		new AddCookiesBuilder(url, cookiesExpression)
	}
}

class AddCookiesBuilder(url: Expression[String], cookies: Expression[List[AHCCookie]]) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new AddCookies(url, cookies, next)))
}