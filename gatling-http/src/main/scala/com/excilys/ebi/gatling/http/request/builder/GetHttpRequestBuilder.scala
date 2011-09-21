package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.MIMEType._

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object GetHttpRequestBuilder {
  class GetHttpRequestBuilder(url: Option[String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], followsRedirects: Option[Boolean], urlInterpolations: Seq[String])
      extends HttpRequestBuilder(url, queryParams, None, headers, None, followsRedirects, urlInterpolations) with Logging {
    def withQueryParam(paramKey: String, paramValue: String) =
      new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, followsRedirects, urlInterpolations)

    def withQueryParam(paramKey: String, paramValue: FromContext) =
      new GetHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, followsRedirects, urlInterpolations)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withHeader(header: Tuple2[String, String]) = new GetHttpRequestBuilder(url, queryParams, Some(headers.get + (header._1 -> header._2)), followsRedirects, urlInterpolations)

    def withHeaders(givenHeaders: Map[String, String]) = new GetHttpRequestBuilder(url, queryParams, Some(headers.get ++ givenHeaders), followsRedirects, urlInterpolations)

    def asJSON = new GetHttpRequestBuilder(url, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_JSON)), followsRedirects, urlInterpolations)

    def asXML = new GetHttpRequestBuilder(url, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_XML)), followsRedirects, urlInterpolations)

    def followsRedirect(followRedirect: Boolean) = new GetHttpRequestBuilder(url, queryParams, headers, Some(followRedirect), urlInterpolations)

    def build(context: Context): Request = build(context, "GET")
  }

  def get(url: String, urlInterpolations: String*) = new GetHttpRequestBuilder(Some(url), Some(Map()), Some(Map()), None, urlInterpolations)
}