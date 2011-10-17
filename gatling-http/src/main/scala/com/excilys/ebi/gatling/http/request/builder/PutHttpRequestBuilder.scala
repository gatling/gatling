package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder

class PutHttpRequestBuilder(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
	headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[Tuple2[String, String]])
		extends AbstractHttpRequestWithBodyBuilder[PutHttpRequestBuilder](httpRequestActionBuilder, urlFormatter, queryParams, headers, body, followsRedirects, credentials) {

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[Tuple2[String, String]]) = {
		new PutHttpRequestBuilder(httpRequestActionBuilder, urlFormatter, queryParams, headers, body, followsRedirects, credentials)
	}

	def getMethod = "PUT"
}
