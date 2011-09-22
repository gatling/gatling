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
import com.excilys.ebi.gatling.http.request.MIMEType._

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import java.io.File

import org.fusesource.scalate._

object DeleteHttpRequestBuilder {
  class DeleteHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]],
                                 body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
      extends HttpRequestBuilder(urlFormatter, queryParams, None, headers, body, followsRedirects) with Logging {
    def withQueryParam(paramKey: String, paramValue: String) = new DeleteHttpRequestBuilder(urlFormatter, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, body, followsRedirects)

    def withQueryParam(paramKey: String, paramValue: FromContext) = new DeleteHttpRequestBuilder(urlFormatter, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, body, followsRedirects)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withHeader(header: Tuple2[String, String]) = new DeleteHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get + (header._1 -> header._2)), body, followsRedirects)

    def withHeaders(givenHeaders: Map[String, String]) = new DeleteHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get ++ givenHeaders), body, followsRedirects)

    def asJSON = new DeleteHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_JSON) + (CONTENT_TYPE -> APPLICATION_JSON)), body, followsRedirects)

    def asXML = new DeleteHttpRequestBuilder(urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_XML) + (CONTENT_TYPE -> APPLICATION_XML)), body, followsRedirects)

    def withFile(filePath: String) = new DeleteHttpRequestBuilder(urlFormatter, queryParams, headers, Some(FilePathBody(filePath)), followsRedirects)

    def withBody(body: String) = new DeleteHttpRequestBuilder(urlFormatter, queryParams, headers, Some(StringBody(body)), followsRedirects)

    def withTemplateBodyFromContext(tplPath: String, values: Map[String, String]) = new DeleteHttpRequestBuilder(urlFormatter, queryParams, headers, Some(TemplateBody(tplPath, values.map { value => (value._1, ContextParam(value._2)) })), followsRedirects)

    def withTemplateBody(tplPath: String, values: Map[String, String]) = new DeleteHttpRequestBuilder(urlFormatter, queryParams, headers, Some(TemplateBody(tplPath, values.map { value => (value._1, StringParam(value._2)) })), followsRedirects)

    def followsRedirect(followRedirect: Boolean) = new DeleteHttpRequestBuilder(urlFormatter, queryParams, headers, body, Some(followRedirect))

    def build(context: Context): Request = build(context, "DELETE")
  }

  def delete(url: String, interpolations: String*) = new DeleteHttpRequestBuilder(Some((c: Context) => interpolate(c, url, interpolations)), Some(Map()), Some(Map()), None, None)
  def delete(f: Context => String) = new DeleteHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), None, None)
}