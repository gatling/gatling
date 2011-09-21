package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.feeder.Feeder

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.builder.mixin.Body
import com.excilys.ebi.gatling.http.request.MIMEType._

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import java.io.File

import org.fusesource.scalate._

object PutHttpRequestBuilder {
  class PutHttpRequestBuilder(url: Option[String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]],
                              body: Option[HttpRequestBody], feeder: Option[Feeder], followsRedirects: Option[Boolean], urlInterpolations: Seq[String])
      extends HttpRequestBuilder(url, queryParams, None, headers, body, feeder, followsRedirects, urlInterpolations) with Body with Logging {
    def withQueryParam(paramKey: String, paramValue: String) = new PutHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, body, feeder, followsRedirects, urlInterpolations)

    def withQueryParam(paramKey: String, paramValue: FromContext) = new PutHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, body, feeder, followsRedirects, urlInterpolations)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withHeader(header: Tuple2[String, String]) = new PutHttpRequestBuilder(url, queryParams, Some(headers.get + (header._1 -> header._2)), body, feeder, followsRedirects, urlInterpolations)

    def withHeaders(givenHeaders: Map[String, String]) = new PutHttpRequestBuilder(url, queryParams, Some(headers.get ++ givenHeaders), body, feeder, followsRedirects, urlInterpolations)

    def asJSON = new PutHttpRequestBuilder(url, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_JSON) + (CONTENT_TYPE -> APPLICATION_JSON)), body, feeder, followsRedirects, urlInterpolations)

    def asXML = new PutHttpRequestBuilder(url, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_XML) + (CONTENT_TYPE -> APPLICATION_XML)), body, feeder, followsRedirects, urlInterpolations)

    def withFile(filePath: String) = new PutHttpRequestBuilder(url, queryParams, headers, Some(FilePathBody(filePath)), feeder, followsRedirects, urlInterpolations)

    def withBody(body: String) = new PutHttpRequestBuilder(url, queryParams, headers, Some(StringBody(body)), feeder, followsRedirects, urlInterpolations)

    def withTemplateBodyFromContext(tplPath: String, values: Map[String, String]) = new PutHttpRequestBuilder(url, queryParams, headers, Some(TemplateBody(tplPath, values.map { value => (value._1, ContextParam(value._2)) })), feeder, followsRedirects, urlInterpolations)

    def withTemplateBody(tplPath: String, values: Map[String, String]) = new PutHttpRequestBuilder(url, queryParams, headers, Some(TemplateBody(tplPath, values.map { value => (value._1, StringParam(value._2)) })), feeder, followsRedirects, urlInterpolations)

    def withFeeder(feeder: Feeder) = new PutHttpRequestBuilder(url, queryParams, headers, body, Some(feeder), followsRedirects, urlInterpolations)

    def followsRedirect(followRedirect: Boolean) = new PutHttpRequestBuilder(url, queryParams, headers, body, feeder, Some(followRedirect), urlInterpolations)

    def build(context: Context): Request = build(context, "PUT")
  }

  def put(url: String, urlInterpolations: String*) = new PutHttpRequestBuilder(Some(url), Some(Map()), Some(Map()), None, None, None, urlInterpolations)
}