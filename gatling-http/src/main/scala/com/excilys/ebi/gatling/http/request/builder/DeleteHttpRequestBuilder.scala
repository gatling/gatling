package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param

object DeleteHttpRequestBuilder {
  class DeleteHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
                                 headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
      extends AbstractHttpRequestWithBodyBuilder[DeleteHttpRequestBuilder](urlFormatter, queryParams, headers, body, followsRedirects) {

    def newInstance(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]) = {
      new DeleteHttpRequestBuilder(urlFormatter, queryParams, headers, body, followsRedirects)
    }

    def getMethod = "DELETE"
  }

  def delete(url: String, interpolations: String*) = new DeleteHttpRequestBuilder(Some((c: Context) => interpolate(c, url, interpolations)), Some(Map()), Some(Map()), None, None)
  def delete(f: Context => String) = new DeleteHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), None, None)
}