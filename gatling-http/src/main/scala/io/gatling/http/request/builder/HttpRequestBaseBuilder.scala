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

import io.gatling.core.session.{ EL, Expression }

/**
 * HttpRequestActionBuilder class companion
 */
object HttpRequestBaseBuilder {

	/**
	 * This method is used in DSL to declare a new HTTP request
	 */
	def http(requestName: Expression[String]) = new HttpRequestBaseBuilder(requestName)
}

/**
 * Builder for HttpRequestActionBuilder
 *
 * @constructor creates an HttpRequestActionBuilder
 * @param requestName the name of the request
 */
class HttpRequestBaseBuilder(requestName: Expression[String]) {

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param url the function returning the url of this request
	 */
	def delete(url: Expression[String]) = DeleteHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param url the function returning the url of this request
	 */
	def get(url: Expression[String]) = GetHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word PATCH
	 *
	 * @param url the function returning the url of this request
	 */
	def patch(url: Expression[String]) = PatchHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param url the function returning the url of this request
	 */
	def post(url: Expression[String]) = PostHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param url the function returning the url of this request
	 */
	def put(url: Expression[String]) = PutHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word HEAD
	 *
	 * @param url the function returning the url of this request
	 */
	def head(url: Expression[String]) = HeadHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word OPTIONS
	 *
	 * @param url the function returning the url of this request
	 */
	def options(url: Expression[String]) = OptionsHttpRequestBuilder(requestName, url)
}

