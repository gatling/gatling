package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.MIMEType._

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object GetHttpRequestBuilder {
  class GetHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], followsRedirects: Option[Boolean])
      extends HttpRequestBuilder(urlFormatter, queryParams, None, headers, None, followsRedirects) with Logging {
    def withQueryParam(paramKey: String, paramValue: String) =
      new GetHttpRequestBuilder(urlFormatter, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, followsRedirects)

    def withQueryParam(paramKey: String, paramValue: FromContext) =
      new GetHttpRequestBuilder(urlFormatter, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, followsRedirects)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withHeader(header: Tuple2[String, String]) = new GetHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get + (header._1 -> header._2)), followsRedirects)

    def withHeaders(givenHeaders: Map[String, String]) = new GetHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get ++ givenHeaders), followsRedirects)

    def asJSON = new GetHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_JSON)), followsRedirects)

    def asXML = new GetHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_XML)), followsRedirects)

    def followsRedirect(followRedirect: Boolean) = new GetHttpRequestBuilder(urlFormatter, queryParams, headers, Some(followRedirect))

    def build(context: Context): Request = build(context, "GET")
  }

  def get(url: String, interpolations: String*) = new GetHttpRequestBuilder(Some((c: Context) => interpolate(c, url, interpolations)), Some(Map()), Some(Map()), None)
  def get(f: Context => String) = new GetHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), None)
}