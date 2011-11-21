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
import com.excilys.ebi.gatling.core.context.SavedValue
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.ning.http.client.RequestBuilder

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param urlFunction the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param params the parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param body the body that should be added to the request
 * @param followsRedirects sets the follow redirect option of AHC
 * @param credentials sets the credentials in case of Basic HTTP Authentication
 */
abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](httpRequestActionBuilder: HttpRequestActionBuilder,
	urlFunction: Option[Context => String], queryParams: List[(Context => String, Context => Option[String])], params: List[(Context => String, Context => String)], headers: Map[String, String], body: Option[HttpRequestBody],
	followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends AbstractHttpRequestWithBodyBuilder[B](httpRequestActionBuilder, urlFunction, queryParams, headers, body, followsRedirects, credentials) {

	override def getRequestBuilder(context: Context): RequestBuilder = {
		val requestBuilder = super.getRequestBuilder(context)
		logger.debug("Building in with body and params")
		addParamsTo(requestBuilder, params, context)
		requestBuilder
	}

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
	 * @param urlFunction the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param params the parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param body the body that should be added to the request
	 * @param followsRedirects sets the follow redirect option of AHC
	 * @param credentials sets the credentials in case of Basic HTTP Authentication
	 */
	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: List[(Context => String, Context => Option[String])], params: List[(Context => String, Context => String)], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: List[(Context => String, Context => Option[String])], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B = {
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, params, headers, body, followsRedirects, credentials)
	}

	/**
	 *
	 */
	def param(paramKeyFunction: Context => String, paramValueFunction: Context => String): B =
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, (paramKeyFunction, paramValueFunction) :: params, headers, body, followsRedirects, credentials)

	/**
	 * Adds a parameter to the request
	 *
	 * @param paramKey the key of the parameter
	 * @param paramValue the value of the parameter
	 */
	def param(paramKey: String, paramValue: String): B = param((c: Context) => paramKey, (c: Context) => paramValue)

	/**
	 * Adds a parameter to the request
	 *
	 * @param paramKey the ley of the parameter
	 * @param paramValue a SavedValue(contextKey) that indicates the context key from which the value should be extracted
	 */
	def param(paramKey: String, paramValue: SavedValue): B = param((c: Context) => paramKey, (c: Context) => c.getAttribute(paramValue.attributeKey).toString)

	def param(paramKey: SavedValue, paramValue: String): B = param((c: Context) => c.getAttribute(paramKey.attributeKey).toString, (c: Context) => paramValue)

	def param(paramKey: SavedValue, paramValue: SavedValue): B = param((c: Context) => c.getAttribute(paramKey.attributeKey).toString, (c: Context) => c.getAttribute(paramValue.attributeKey).toString)

	/**
	 * This method adds the parameters to the request builder
	 *
	 * @param requestBuilder the request builder to which the parameters should be added
	 * @param params the parameters that should be added
	 * @param context the context of the current scenario
	 */
	private def addParamsTo(requestBuilder: RequestBuilder, params: List[(Context => String, Context => String)], context: Context) = {
		for ((keyFunction, valueFunction) <- params) {
			requestBuilder addParameter (keyFunction.apply(context), valueFunction.apply(context))
		}
	}
}