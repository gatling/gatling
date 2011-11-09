/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param

/**
 * This class defines an HTTP request with word DELETE in the DSL
 */
class DeleteHttpRequestBuilder(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: Map[String, Param],
	headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends AbstractHttpRequestWithBodyBuilder[DeleteHttpRequestBuilder](httpRequestActionBuilder, urlFunction, queryParams, headers, body, followsRedirects, credentials) {

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: Map[String, Param], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]) = {
		new DeleteHttpRequestBuilder(httpRequestActionBuilder, urlFunction, queryParams, headers, body, followsRedirects, credentials)
	}

	def getMethod = "DELETE"
}

