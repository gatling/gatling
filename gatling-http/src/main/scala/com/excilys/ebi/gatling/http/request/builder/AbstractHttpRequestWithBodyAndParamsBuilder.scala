package com.excilys.ebi.gatling.http.request.builder

import com.ning.http.client.Request
import com.ning.http.client.RequestBuilder
import com.ning.http.client.FluentStringsMap
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder

abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], params: Option[Map[String, Param]],
	headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
		extends AbstractHttpRequestWithBodyBuilder[B](httpRequestActionBuilder, urlFormatter, queryParams, headers, body, followsRedirects) {

	override def getRequestBuilder(context: Context): RequestBuilder = {
		val requestBuilder = super.getRequestBuilder(context)
		addParamsTo(requestBuilder, params, context)
		requestBuilder
	}

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], params: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]): B

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]): B = {
		newInstance(httpRequestActionBuilder, urlFormatter, queryParams, params, headers, body, followsRedirects)
	}

	def param(paramKey: String, paramValue: String): B = {
		newInstance(httpRequestActionBuilder, urlFormatter, queryParams, Some(params.get + (paramKey -> StringParam(paramValue))), headers, body, followsRedirects)
	}

	def param(paramKey: String, paramValue: FromContext): B = {
		newInstance(httpRequestActionBuilder, urlFormatter, queryParams, Some(params.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, body, followsRedirects)
	}

	def param(paramKey: String): B = param(paramKey, FromContext(paramKey))

	private def addParamsTo(requestBuilder: RequestBuilder, params: Option[Map[String, Param]], context: Context) = {
		requestBuilder setParameters new FluentStringsMap
		for (param <- params.get) {
			param._2 match {
				case StringParam(string) => requestBuilder addParameter (param._1, string)
				case ContextParam(string) => requestBuilder addParameter (param._1, context.getAttribute(string))
			}
		}
	}
}