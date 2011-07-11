package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object GetHttpRequestBuilder {
  class GetHttpRequestBuilder(val url: Option[String], params: Option[Map[String, String]]) extends HttpRequestBuilder with Logging {
    def withParam(param: Tuple2[String, String]) = new GetHttpRequestBuilder(url, Some(params.get + (param._1 -> param._2)))

    def build(): Request = {
      val requestBuilder = new RequestBuilder setUrl url.get
      for (param <- params.get) {
        requestBuilder addQueryParameter (param._1, param._2)
      }
      logger.debug("Built GET Request")

      requestBuilder build
    }
  }

  def get(url: String) = new GetHttpRequestBuilder(Some(url), Some(Map()))
}