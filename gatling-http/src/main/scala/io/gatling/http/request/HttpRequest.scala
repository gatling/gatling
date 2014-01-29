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
/**
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
package io.gatling.http.request

import com.ning.http.client.Request
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocol
import io.gatling.http.fetch.NamedRequest
import io.gatling.http.response.ResponseTransformer

object HttpRequest extends StrictLogging {

	def buildNamedRequests(resources: Seq[HttpRequest], session: Session): List[NamedRequest] = resources.foldLeft(List.empty[NamedRequest]) { (acc, res) =>

		val namedRequest = for {
			name <- res.requestName(session)
			request <- res.ahcRequest(session)
		} yield NamedRequest(name, request)

		namedRequest match {
			case Success(request) => request :: acc
			case Failure(message) =>
				logger.warn(s"Couldn't fetch resource: $message")
				acc
		}
	}.reverse
}

case class HttpRequest(
	requestName: Expression[String],
	ahcRequest: Expression[Request],
	checks: List[HttpCheck],
	responseTransformer: Option[ResponseTransformer],
	extraInfoExtractor: Option[ExtraInfoExtractor],
	maxRedirects: Option[Int],
	throttled: Boolean,
	protocol: HttpProtocol,
	explicitResources: Seq[HttpRequest])
