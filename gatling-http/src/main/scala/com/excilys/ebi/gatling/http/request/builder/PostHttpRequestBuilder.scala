package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.builder.mixin.RequestParamsAndBody
import com.excilys.ebi.gatling.http.request.MIMEType._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import java.io.File

import org.fusesource.scalate._

object PostHttpRequestBuilder {
  class PostHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], params: Option[Map[String, Param]],
                               headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
      extends HttpRequestBuilder(urlFormatter, queryParams, params, headers, body, followsRedirects) with RequestParamsAndBody with Logging {

    def withQueryParam(paramKey: String, paramValue: String) = new PostHttpRequestBuilder(urlFormatter, Some(queryParams.get + (paramKey -> StringParam(paramValue))), params, headers, body, followsRedirects)

    def withQueryParam(paramKey: String, paramValue: FromContext) = new PostHttpRequestBuilder(urlFormatter, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), params, headers, body, followsRedirects)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withParam(paramKey: String, paramValue: String) = new PostHttpRequestBuilder(urlFormatter, queryParams, Some(params.get + (paramKey -> StringParam(paramValue))), headers, body, followsRedirects)

    def withParam(paramKey: String, paramValue: FromContext) = new PostHttpRequestBuilder(urlFormatter, queryParams, Some(params.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, body, followsRedirects)

    def withParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withHeader(header: Tuple2[String, String]) = new PostHttpRequestBuilder(urlFormatter, queryParams, params, Some(headers.get + (header._1 -> header._2)), body, followsRedirects)

    def withHeaders(givenHeaders: Map[String, String]) = new PostHttpRequestBuilder(urlFormatter, queryParams, params, Some(headers.get ++ givenHeaders), body, followsRedirects)

    def asJSON = new PostHttpRequestBuilder(urlFormatter, queryParams, params, Some(headers.get + (ACCEPT -> APPLICATION_JSON) + (CONTENT_TYPE -> APPLICATION_JSON)), body, followsRedirects)

    def asXML = new PostHttpRequestBuilder(urlFormatter, queryParams, params, Some(headers.get + (ACCEPT -> APPLICATION_XML) + (CONTENT_TYPE -> APPLICATION_XML)), body, followsRedirects)

    def withFile(filePath: String) = new PostHttpRequestBuilder(urlFormatter, queryParams, params, headers, Some(FilePathBody(filePath)), followsRedirects)

    def withBody(body: String) = new PostHttpRequestBuilder(urlFormatter, queryParams, params, headers, Some(StringBody(body)), followsRedirects)

    def withTemplateBodyFromContext(tplPath: String, values: Map[String, String]) = new PostHttpRequestBuilder(urlFormatter, queryParams, params, headers, Some(TemplateBody(tplPath, values.map { value => (value._1, ContextParam(value._2)) })), followsRedirects)

    def withTemplateBody(tplPath: String, values: Map[String, String]) = new PostHttpRequestBuilder(urlFormatter, queryParams, params, headers, Some(TemplateBody(tplPath, values.map { value => (value._1, StringParam(value._2)) })), followsRedirects)

    def followsRedirect(followRedirect: Boolean) = new PostHttpRequestBuilder(urlFormatter, queryParams, params, headers, body, Some(followRedirect))

    def build(context: Context): Request = build(context, "POST")
  }

  def post(url: String, interpolations: String*) = new PostHttpRequestBuilder(Some((c: Context) => interpolate(c, url, interpolations)), Some(Map()), Some(Map()), Some(Map()), None, None)
  def post(f: Context => String) = new PostHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), Some(Map()), None, None)
}