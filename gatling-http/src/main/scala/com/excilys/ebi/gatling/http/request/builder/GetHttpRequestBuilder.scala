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
  class GetHttpRequestBuilder(url: Option[String], queryParams: Option[Map[String, Param]], feeder: Option[Feeder])
      extends HttpRequestBuilder(url, queryParams, feeder) with Logging {
    def withQueryParam(paramKey: String, paramValue: String) = new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))), feeder)

    def withQueryParam(paramKey: String, paramValue: FromContext) = new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), feeder)

    def withFeeder(feeder: Feeder) = new GetHttpRequestBuilder(url, queryParams, Some(feeder))

    def build(context: Context): Request = {
      feeder.map { f =>
        context.setAttributes(f.next)
      }

      val requestBuilder = new RequestBuilder setUrl url.get
      for (cookie <- context.getCookies) {
        requestBuilder.addCookie(cookie)
      }

      for (queryParam <- queryParams.get) {
        queryParam._2 match {
          case StringParam(string) => requestBuilder addQueryParameter (queryParam._1, string)
          case ContextParam(string) => requestBuilder addQueryParameter (queryParam._1, context.getAttribute(string))
        }
      }
      logger.debug("Built GET Request")

      requestBuilder build
    }
  }

  def get(url: String) = new GetHttpRequestBuilder(Some(url), Some(Map()), None)
}