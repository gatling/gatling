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
package com.excilys.ebi.gatling.http.referer

import scala.collection.JavaConversions.asScalaBuffer

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.ning.http.client.Request

object RefererHandling {

	val REFERER_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.referer"

	def getStoredReferer(session: Session): Option[String] = session.getAs[String](REFERER_CONTEXT_KEY)

	def addStoredRefererHeader(headers: Map[String, String], session: Session, protocolConfiguration: HttpProtocolConfiguration): Map[String, String] = getStoredReferer(session) match {
		case Some(referer) if (protocolConfiguration.automaticRefererEnabled && !headers.contains(Headers.Names.REFERER)) => headers + (Headers.Names.REFERER -> referer)
		case _ => headers
	}

	def storeReferer(request: Request, session: Session, protocolConfiguration: HttpProtocolConfiguration): Session = {

		def isRealPage(request: Request): Boolean = !request.getHeaders.containsKey(Headers.Names.X_REQUESTED_WITH) && Option(request.getHeaders.get(Headers.Names.ACCEPT)).map(_.head.contains("html")).isDefined

		if (protocolConfiguration.automaticRefererEnabled && isRealPage(request)) session.set(REFERER_CONTEXT_KEY, request.getUrl) else session
	}
}