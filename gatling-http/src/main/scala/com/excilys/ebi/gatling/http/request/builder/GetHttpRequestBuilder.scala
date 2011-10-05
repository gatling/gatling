package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder

class GetHttpRequestBuilder(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
                            headers: Option[Map[String, String]], followsRedirects: Option[Boolean])
    extends AbstractHttpRequestBuilder[GetHttpRequestBuilder](httpRequestActionBuilder, urlFormatter, queryParams, headers, followsRedirects) {

  def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], followsRedirects: Option[Boolean]) = {
    new GetHttpRequestBuilder(httpRequestActionBuilder, urlFormatter, queryParams, headers, followsRedirects)
  }

  def getMethod = "GET"
}
