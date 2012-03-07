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
package com.excilys.ebi.gatling.http.request.builder
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.check.HttpCheck

/**
 * This class defines an HTTP request with word PUT in the DSL
 */
class PutHttpRequestBuilder(
	requestName: String,
	url: EvaluatableString,
	queryParams: List[HttpParam],
	headers: Map[String, EvaluatableString],
	body: Option[HttpRequestBody],
	credentials: Option[Credentials],
	checks: Option[List[HttpCheck]])
		extends AbstractHttpRequestWithBodyBuilder[PutHttpRequestBuilder](requestName, "PUT", url, queryParams, headers, body, credentials, checks) {

	private[http] def newInstance(
		requestName: String,
		url: EvaluatableString,
		queryParams: List[HttpParam],
		headers: Map[String, EvaluatableString],
		body: Option[HttpRequestBody],
		credentials: Option[Credentials],
		checks: Option[List[HttpCheck]]) = {
		new PutHttpRequestBuilder(requestName, url, queryParams, headers, body, credentials, checks)
	}
}
