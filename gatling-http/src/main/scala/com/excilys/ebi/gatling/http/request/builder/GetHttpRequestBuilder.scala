package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.feeder.Feeder

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.FeederParam
import com.excilys.ebi.gatling.http.request.ContextParam

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object GetHttpRequestBuilder {
  class GetHttpRequestBuilder(url: Option[String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], feeder: Option[Feeder])
      extends HttpRequestBuilder(url, queryParams, None, headers, None, feeder) with Logging {
    def withQueryParam(paramKey: String, paramValue: String) =
      new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, feeder)

    def withQueryParam(paramKey: String, paramValue: FromContext) =
      new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, feeder)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withFeeder(feeder: Feeder) = new GetHttpRequestBuilder(url, queryParams, headers, Some(feeder))

    def withHeader(header: Tuple2[String, String]) = new GetHttpRequestBuilder(url, queryParams, Some(headers.get + (header._1 -> header._2)), feeder)

    def asJSON = new GetHttpRequestBuilder(url, queryParams, Some(headers.get + ("Accept" -> "application/json")), feeder)

    def asXML = new GetHttpRequestBuilder(url, queryParams, Some(headers.get + ("Accept" -> "application/xml")), feeder)

    def build(context: Context): Request = build(context, "GET")
  }

  def get(url: String) = new GetHttpRequestBuilder(Some(url), Some(Map()), Some(Map()), None)
}