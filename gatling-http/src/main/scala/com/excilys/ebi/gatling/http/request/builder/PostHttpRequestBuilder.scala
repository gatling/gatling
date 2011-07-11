package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object PostHttpRequestBuilder {
  class PostHttpRequestBuilder(val url: Option[String], val queryParams: Option[Map[String, String]], val params: Option[Map[String, String]])
    extends HttpRequestBuilder with Logging {

    def withQueryParam(queryParam: Tuple2[String, String]) = new PostHttpRequestBuilder(url, Some(queryParams.get + (queryParam._1 -> queryParam._2)), params)

    def withParam(param: Tuple2[String, String]) = new PostHttpRequestBuilder(url, queryParams, Some(params.get + (param._1 -> param._2)))

    def build(): Request = {
      val requestBuilder = new RequestBuilder setUrl url.get
      for (queryParam <- queryParams.get) {
        requestBuilder addQueryParameter (queryParam._1, queryParam._2)
      }
      for (param <- params.get) {
        requestBuilder addParameter (param._1, param._2)
      }
      logger.debug("Built POST Request")

      requestBuilder setMethod "POST" build
    }
  }

  def post(url: String) = new PostHttpRequestBuilder(Some(url), Some(Map()), Some(Map()))

  def postJSON = {}

  def postXML = {}

  def postGWT = {}

  def postAMF = {}
}