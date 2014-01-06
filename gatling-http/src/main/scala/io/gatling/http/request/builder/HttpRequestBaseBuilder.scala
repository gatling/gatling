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

import java.net.URI

import io.gatling.core.session.Expression

/**
 * @param requestName the name of the request
 */
class HttpRequestBaseBuilder(requestName: Expression[String]) {

	def get(url: Expression[String]) = httpRequest("GET", Left(url))
	def get(uri: URI) = httpRequest("GET", Right(uri))
	def put(url: Expression[String]) = httpRequest("PUT", Left(url))
	def patch(url: Expression[String]) = httpRequest("PATCH", Left(url))
	def head(url: Expression[String]) = httpRequest("HEAD", Left(url))
	def delete(url: Expression[String]) = httpRequest("DELETE", Left(url))
	def options(url: Expression[String]) = httpRequest("OPTIONS", Left(url))
	def httpRequest(method: String, urlOrURI: Either[Expression[String], URI]) = HttpRequestBuilder(method, requestName, urlOrURI)

	def post(url: Expression[String]) = httpRequestWithParams("POST", Left(url))
	def httpRequestWithParams(method: String, urlOrURI: Either[Expression[String], URI]) = HttpRequestWithParamsBuilder(method, requestName, urlOrURI)
}
