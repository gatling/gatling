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
package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.session.ELParser.parseEL
import com.excilys.ebi.gatling.core.session.EvaluatableString

/**
 * HttpRequestActionBuilder class companion
 */
object HttpRequestBaseBuilder {

	/**
	 * This method is used in DSL to declare a new HTTP request
	 */
	def http(requestName: EvaluatableString) = new HttpRequestBaseBuilder(requestName)
}

/**
 * Builder for HttpRequestActionBuilder
 *
 * @constructor creates an HttpRequestActionBuilder
 * @param requestName the name of the request
 */
class HttpRequestBaseBuilder(requestName: EvaluatableString) {

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param url the url on which this request will be made
	 */
	def delete(url: String): DeleteHttpRequestBuilder = delete(parseEL(url))

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param url the function returning the url of this request
	 */
	def delete(url: EvaluatableString) = DeleteHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param url the url on which this request will be made
	 */
	def get(url: String): GetHttpRequestBuilder = get(parseEL(url))

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param url the function returning the url of this request
	 */
	def get(url: EvaluatableString) = GetHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param url the url on which this request will be made
	 */
	def post(url: String): PostHttpRequestBuilder = post(parseEL(url))

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param url the function returning the url of this request
	 */
	def post(url: EvaluatableString) = PostHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param url the url on which this request will be made
	 */
	def put(url: String): PutHttpRequestBuilder = put(parseEL(url))

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param url the function returning the url of this request
	 */
	def put(url: EvaluatableString) = PutHttpRequestBuilder(requestName, url)

	/**
	 * Starts the definition of an HTTP request with word HEAD
	 *
	 * @param url the url on which this request will be made
	 */
	def head(url: String): HeadHttpRequestBuilder = head(parseEL(url))

	/**
	 * Starts the definition of an HTTP request with word HEAD
	 *
	 * @param url the function returning the url of this request
	 */
	def head(url: EvaluatableString) = HeadHttpRequestBuilder(requestName, url)
	
		/**
	 * Starts the definition of an HTTP request with word OPTIONS
	 *
	 * @param url the url on which this request will be made
	 */
	def options(url: String): OptionsHttpRequestBuilder = options(parseEL(url))

	/**
	 * Starts the definition of an HTTP request with word OPTIONS
	 *
	 * @param url the function returning the url of this request
	 */
	def options(url: EvaluatableString) = OptionsHttpRequestBuilder(requestName, url)
}

