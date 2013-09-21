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

object HttpRequestBaseBuilder {

	/**
	 * This method is used in DSL to declare a new HTTP request
	 */
	def http(requestName: Expression[String]) = new HttpRequestBaseBuilder(requestName)
}

/**
 * @param requestName the name of the request
 */
class HttpRequestBaseBuilder(requestName: Expression[String]) {

	def httpRequest(method: String, url: Expression[String]) = HttpRequestBuilder(method, requestName, url)
	def get(url: Expression[String]) = httpRequest("GET", url)
	def delete(url: Expression[String]) = httpRequest("DELETE", url)

	def httpRequestWithBody(method: String, url: Expression[String]) = HttpRequestWithBodyBuilder(method, requestName, url)
	def put(url: Expression[String]) = httpRequestWithBody("PUT", url)
	def patch(url: Expression[String]) = httpRequestWithBody("PATCH", url)
	def head(url: Expression[String]) = httpRequestWithBody("HEAD", url)
	def options(url: Expression[String]) = httpRequestWithBody("OPTIONS", url)

	def httpRequestWithBodyAndParams(method: String, url: Expression[String]) = HttpRequestWithBodyAndParamsBuilder(method, requestName, url)
	def post(url: Expression[String]) = httpRequestWithBodyAndParams("POST", url)
}
