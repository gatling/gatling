package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder

class DeleteHttpRequestBuilder(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
	headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
		extends AbstractHttpRequestWithBodyBuilder[DeleteHttpRequestBuilder](httpRequestActionBuilder, urlFormatter, queryParams, headers, body, followsRedirects) {

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]) = {
		new DeleteHttpRequestBuilder(httpRequestActionBuilder, urlFormatter, queryParams, headers, body, followsRedirects)
	}

	def getMethod = "DELETE"
}

