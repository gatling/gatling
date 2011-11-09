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
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.ning.http.client.RequestBuilder

abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: Map[String, Param], params: Map[String, Param],
	headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends AbstractHttpRequestWithBodyBuilder[B](httpRequestActionBuilder, urlFunction, queryParams, headers, body, followsRedirects, credentials) {

	override def getRequestBuilder(context: Context): RequestBuilder = {
		val requestBuilder = super.getRequestBuilder(context)
		logger.debug("Building in with body and params")
		addParamsTo(requestBuilder, params, context)
		requestBuilder
	}

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: Map[String, Param], params: Map[String, Param], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: Map[String, Param], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B = {
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, params, headers, body, followsRedirects, credentials)
	}

	def param(paramKey: String, paramValue: String): B = {
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, params + (paramKey -> StringParam(paramValue)), headers, body, followsRedirects, credentials)
	}

	def param(paramKey: String, paramValue: FromContext): B = {
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, params + (paramKey -> ContextParam(paramValue.attributeKey)), headers, body, followsRedirects, credentials)
	}

	def param(paramKey: String): B = param(paramKey, FromContext(paramKey))

	private def addParamsTo(requestBuilder: RequestBuilder, params: Map[String, Param], context: Context) = {
		for (param <- params) {
			param._2 match {
				case StringParam(string) => requestBuilder addParameter (param._1, string)
				case ContextParam(string) => requestBuilder addParameter (param._1, context.getAttribute(string).toString)
			}
		}
	}
}