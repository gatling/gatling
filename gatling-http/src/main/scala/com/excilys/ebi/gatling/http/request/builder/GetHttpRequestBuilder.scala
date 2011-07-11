package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object GetHttpRequestBuilder {
  class GetHttpRequestBuilder(val url: Option[String], val queryParams: Option[Map[String, String]]) extends HttpRequestBuilder with Logging {
    def withQueryParam(queryParam: Tuple2[String, String]) = new GetHttpRequestBuilder(url, Some(queryParams.get + (queryParam._1 -> queryParam._2)))

    def build(): Request = {
      val requestBuilder = new RequestBuilder setUrl url.get
      for (queryParam <- queryParams.get) {
        requestBuilder addQueryParameter (queryParam._1, queryParam._2)
      }
      logger.debug("Built GET Request")

      requestBuilder build
    }
  }

  def get(url: String) = new GetHttpRequestBuilder(Some(url), Some(Map()))
}