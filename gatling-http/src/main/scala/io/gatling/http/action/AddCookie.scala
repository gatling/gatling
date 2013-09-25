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

import com.ning.http.client.Cookie

import akka.actor.ActorRef
import io.gatling.core.action.{ Failable, Interruptable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.cookie.CookieHandling

class AddCookie(name: Expression[String], value: Expression[String], domain: Expression[String], path: Expression[String], val next: ActorRef) extends Interruptable with Failable {

	def executeOrFail(session: Session) =
		for {
			name <- name(session)
			value <- value(session)
			domain <- domain(session)
			path <- path(session)
			cookie = new Cookie(name, value, domain, path, Integer.MAX_VALUE, false)
		} yield next ! CookieHandling.storeCookie(session, cookie)
}
