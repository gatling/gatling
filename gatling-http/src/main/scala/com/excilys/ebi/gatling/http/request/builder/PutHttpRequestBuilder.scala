package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param

object PutHttpRequestBuilder {
  class PutHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
                              headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
      extends AbstractHttpRequestWithBodyBuilder[PutHttpRequestBuilder](urlFormatter, queryParams, headers, body, followsRedirects) {

    def newInstance(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]) = {
      new PutHttpRequestBuilder(urlFormatter, queryParams, headers, body, followsRedirects)
    }

    def getMethod = "PUT"
  }

  def put(url: String, interpolations: String*) = new PutHttpRequestBuilder(Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), None, None)
  def put(f: Context => String) = new PutHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), None, None)
}