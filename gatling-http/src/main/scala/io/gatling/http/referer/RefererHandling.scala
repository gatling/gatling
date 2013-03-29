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
package io.gatling.http.referer

import com.ning.http.client.Request

import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.Headers
import io.gatling.http.config.HttpProtocolConfiguration

object RefererHandling {

	val refererAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.referer"

	def getStoredReferer(session: Session): Option[String] = session.get(refererAttributeName)

	def addStoredRefererHeader(headers: Map[String, String], session: Session, protocolConfiguration: HttpProtocolConfiguration): Map[String, String] = getStoredReferer(session) match {
		case Some(referer) if (protocolConfiguration.automaticRefererEnabled && !headers.contains(Headers.Names.REFERER)) => headers + (Headers.Names.REFERER -> referer)
		case _ => headers
	}

	def storeReferer(request: Request, session: Session, protocolConfiguration: HttpProtocolConfiguration): Session = {

		def isRealPage(request: Request): Boolean = !request.getHeaders.containsKey(Headers.Names.X_REQUESTED_WITH) && Option(request.getHeaders.get(Headers.Names.ACCEPT)).map(_.get(0).contains("html")).getOrElse(false)

		if (protocolConfiguration.automaticRefererEnabled && isRealPage(request)) session.set(refererAttributeName, request.getUrl) else session
	}
}