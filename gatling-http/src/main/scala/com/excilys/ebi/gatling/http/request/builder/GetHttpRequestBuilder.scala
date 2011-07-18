package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.FeederParam

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object GetHttpRequestBuilder {
  class GetHttpRequestBuilder(val url: Option[String], val queryParams: Option[Map[String, Param]]) extends HttpRequestBuilder with Logging {
    def withQueryParam(paramKey: String, paramValue: String) = new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))))

    def withQueryParam(paramKey: String, paramValue: Function[Int, String]) = new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> FeederParam(paramValue))))

    def build(feederIndex: Int): Request = {
      val requestBuilder = new RequestBuilder setUrl url.get
      for (queryParam <- queryParams.get) {
        queryParam._2 match {
          case StringParam(string) => requestBuilder addQueryParameter (queryParam._1, string)
          case FeederParam(func) => requestBuilder addQueryParameter (queryParam._1, func(feederIndex))
        }
      }
      logger.debug("Built GET Request")

      requestBuilder build
    }
  }

  def get(url: String) = new GetHttpRequestBuilder(Some(url), Some(Map()))
}