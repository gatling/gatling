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
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable

/**
 * HttpRequestActionBuilder class companion
 */
object HttpRequestBaseBuilder {

	/**
	 * This method is used in DSL to declare a new HTTP request
	 */
	def http(requestName: String) = new HttpRequestBaseBuilder(requestName)
}

/**
 * Builder for HttpRequestActionBuilder
 *
 * @constructor creates an HttpRequestActionBuilder
 * @param requestName the name of the request
 * @param request the actual HTTP request that will be sent
 * @param next the next action to be executed
 * @param processorBuilders
 */
class HttpRequestBaseBuilder(val requestName: String) {

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def delete(url: String) = new DeleteHttpRequestBuilder(requestName, parseEvaluatable(url), Nil, Map.empty, None, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param f the function returning the url of this request
	 */
	def delete(f: EvaluatableString) = new DeleteHttpRequestBuilder(requestName, f, Nil, Map.empty, None, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def get(url: String) = new GetHttpRequestBuilder(requestName, parseEvaluatable(url), Nil, Map.empty, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param f the function returning the url of this request
	 */
	def get(f: EvaluatableString) = new GetHttpRequestBuilder(requestName, f, Nil, Map.empty, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def post(url: String) = new PostHttpRequestBuilder(requestName, parseEvaluatable(url), Nil, Nil, Map.empty, None, None, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param f the function returning the url of this request
	 */
	def post(f: EvaluatableString) = new PostHttpRequestBuilder(requestName, f, Nil, Nil, Map.empty, None, None, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def put(url: String) = new PutHttpRequestBuilder(requestName, parseEvaluatable(url), Nil, Map.empty, None, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param f the function returning the url of this request
	 */
	def put(f: EvaluatableString) = new PutHttpRequestBuilder(requestName, f, Nil, Map.empty, None, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word HEAD
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def head(url: String) = new HeadHttpRequestBuilder(requestName, parseEvaluatable(url), Nil, Map.empty, None, Nil)

	/**
	 * Starts the definition of an HTTP request with word HEAD
	 *
	 * @param f the function returning the url of this request
	 */
	def head(f: EvaluatableString) = new HeadHttpRequestBuilder(requestName, f, Nil, Map.empty, None, Nil)
}

