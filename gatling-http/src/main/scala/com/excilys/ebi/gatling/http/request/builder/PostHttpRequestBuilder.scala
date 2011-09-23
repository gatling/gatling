package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.MIMEType._

object PostHttpRequestBuilder {
  class PostHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], params: Option[Map[String, Param]],
                               headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
      extends AbstractHttpRequestWithBodyAndParamsBuilder[PostHttpRequestBuilder](urlFormatter, queryParams, params, headers, body, followsRedirects) {

    def newInstance(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], params: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]) = {
      new PostHttpRequestBuilder(urlFormatter, queryParams, params, headers, body, followsRedirects)
    }

    def getMethod = "POST"
  }

  def post(url: String, interpolations: String*) = new PostHttpRequestBuilder(Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), Some(Map()), None, None)
  def post(f: Context => String) = new PostHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), Some(Map()), None, None)
}