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

import com.ning.http.client.{ Cookie => AHCCookie }

import akka.actor.ActorRef
import io.gatling.core.action.{ Interruptable, Failable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.cookie.CookieHandling

class AddCookies(url: Expression[String], cookies: Expression[List[AHCCookie]], val next: ActorRef) extends Interruptable with Failable {

	def executeOrFail(session: Session) =
		for {
			url <- url(session)
			cookies <- cookies(session)
		} yield next ! CookieHandling.storeCookies(session, URI.create(url), cookies)
}