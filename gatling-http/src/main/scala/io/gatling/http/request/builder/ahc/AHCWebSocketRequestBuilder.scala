/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder.ahc

import io.gatling.core.session.Session
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.CommonAttributes

class AHCWebSocketRequestBuilder(commonAttributes: CommonAttributes, session: Session, protocol: HttpProtocol) extends AHCRequestBuilder(commonAttributes, session, protocol) {

	def makeAbsolute(url: String): Validation[String] =
		if (url.startsWith("ws"))
			url.success
		else
			protocol.wsPart.wsBaseURL match {
				case Some(baseURL) => (baseURL + url).success
				case _ => s"No protocol.baseURL defined but provided url is relative : $url".failure
			}
}
