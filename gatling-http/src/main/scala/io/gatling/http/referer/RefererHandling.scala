/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import io.gatling.http.HeaderNames
import io.gatling.http.config.HttpProtocol

object RefererHandling {

  val refererAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.referer"

  def getStoredReferer(session: Session): Option[String] = session(refererAttributeName).asOption[String]

  def storeReferer(request: Request, session: Session, protocol: HttpProtocol): Session = {

      def isRealPage(request: Request): Boolean = !request.getHeaders.containsKey(HeaderNames.X_REQUESTED_WITH) && Option(request.getHeaders.get(HeaderNames.ACCEPT)).exists(_.get(0).contains("html"))

    if (protocol.requestPart.autoReferer && isRealPage(request)) session.set(refererAttributeName, request.getUrl) else session
  }
}
