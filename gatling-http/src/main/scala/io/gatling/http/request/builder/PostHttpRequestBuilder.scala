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

import io.gatling.core.session.{ ELCompiler, Expression, Session }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.RequestBody

object PostHttpRequestBuilder {

	def apply(requestName: Expression[String], url: Expression[String]) = new PostHttpRequestBuilder(HttpAttributes(requestName, "POST", url), None, HttpParamsAttributes())

	def warmUp {
		val expression = "foo".el[String]
		PostHttpRequestBuilder(expression, expression)
			.header("bar", expression)
			.param(expression, expression)
			.build(Session("scenarioName", 0), HttpProtocol.default)
	}
}

/**
 * This class defines an HTTP request with word POST in the DSL
 */
class PostHttpRequestBuilder(
	httpAttributes: HttpAttributes,
	body: Option[RequestBody],
	paramsAttributes: HttpParamsAttributes)
	extends AbstractHttpRequestWithBodyAndParamsBuilder[PostHttpRequestBuilder](httpAttributes, body, paramsAttributes) {

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		body: Option[RequestBody],
		paramsAttributes: HttpParamsAttributes) = new PostHttpRequestBuilder(httpAttributes, body, paramsAttributes)
}
