package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.feeder.Feeder

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.FeederParam
import com.excilys.ebi.gatling.http.request.ContextParam

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import java.io.File

import org.fusesource.scalate._

object DeleteHttpRequestBuilder {
  class DeleteHttpRequestBuilder(url: Option[String], queryParams: Option[Map[String, Param]], val headers: Option[Map[String, String]],
                                 val body: Option[HttpRequestBody], feeder: Option[Feeder])
      extends HttpRequestBuilder(url, queryParams, feeder) with Logging {
    def withQueryParam(paramKey: String, paramValue: String) = new DeleteHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, body, feeder)

    def withQueryParam(paramKey: String, paramValue: FromContext) = new DeleteHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, body, feeder)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withHeader(header: Tuple2[String, String]) = new DeleteHttpRequestBuilder(url, queryParams, Some(headers.get + (header._1 -> header._2)), body, feeder)

    def asJSON = new DeleteHttpRequestBuilder(url, queryParams, Some(headers.get + ("Accept" -> "application/json") + ("Content-Type" -> "application/json")), body, feeder)

    def asXML = new DeleteHttpRequestBuilder(url, queryParams, Some(headers.get + ("Accept" -> "application/xml") + ("Content-Type" -> "application/xml")), body, feeder)

    def withFile(filePath: String) = new DeleteHttpRequestBuilder(url, queryParams, headers, Some(FilePathBody(filePath)), feeder)

    def withBody(body: String) = new DeleteHttpRequestBuilder(url, queryParams, headers, Some(StringBody(body)), feeder)

    def withTemplateBody(tplPath: String, values: Map[String, String]) = new DeleteHttpRequestBuilder(url, queryParams, headers, Some(TemplateBody(tplPath, values)), feeder)

    def withFeeder(feeder: Feeder) = new DeleteHttpRequestBuilder(url, queryParams, headers, body, Some(feeder))

    def build(context: Context): Request = {
      val requestBuilder = new RequestBuilder setUrl url.get setMethod "DELETE"

      consumeSeed(feeder, context)
      addCookiesTo(requestBuilder, context)
      addQueryParamsTo(requestBuilder, context)
      addHeadersTo(requestBuilder, headers)
      addBodyTo(requestBuilder, body)

      logger.debug("Built DELETE Request")
      requestBuilder build
    }
  }

  def delete(url: String) = new DeleteHttpRequestBuilder(Some(url), Some(Map()), Some(Map()), None, None)
}