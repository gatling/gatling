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
package io.gatling.http.request.builder

import io.gatling.core.session.Expression

object PutHttpRequestBuilder {

	def apply(requestName: Expression[String], url: Expression[String]) = new PutHttpRequestBuilder(HttpAttributes(requestName, "PUT", url), BodyAttributes())
}

/**
 * This class defines an HTTP request with word PUT in the DSL
 */
class PutHttpRequestBuilder(
	httpAttributes: HttpAttributes,
	bodyAttributes: BodyAttributes)
		extends AbstractHttpRequestWithBodyBuilder[PutHttpRequestBuilder](httpAttributes, bodyAttributes) {

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		bodyAttributes: BodyAttributes) = {
		new PutHttpRequestBuilder(httpAttributes, bodyAttributes)
	}
}
