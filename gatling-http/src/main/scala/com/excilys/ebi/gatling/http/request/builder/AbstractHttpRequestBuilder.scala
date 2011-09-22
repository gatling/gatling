package com.excilys.ebi.gatling.http.request.builder

import com.ning.http.client.Request
import com.ning.http.client.RequestBuilder
import com.ning.http.client.FluentStringsMap
import com.ning.http.client.FluentCaseInsensitiveStringsMap

import org.fusesource.scalate._

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.MIMEType._

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import java.io.File

abstract class AbstractHttpRequestBuilder[B <: AbstractHttpRequestBuilder[B]](val urlFormatter: Option[Context => String], val queryParams: Option[Map[String, Param]],
                                                                              val headers: Option[Map[String, String]], val followsRedirects: Option[Boolean])
    extends Logging {

  val requestBuilder = new RequestBuilder

  def newInstance(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], followsRedirects: Option[Boolean]): B

  def withQueryParam(paramKey: String, paramValue: String): B = {
    newInstance(urlFormatter, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, followsRedirects)
  }

  def withQueryParam(paramKey: String, paramValue: FromContext): B = {
    newInstance(urlFormatter, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, followsRedirects)
  }

  def withQueryParam(paramKey: String): B = withQueryParam(paramKey, FromContext(paramKey))

  def withHeader(header: Tuple2[String, String]): B = {
    newInstance(urlFormatter, queryParams, Some(headers.get + (header._1 -> header._2)), followsRedirects)
  }

  def withHeaders(givenHeaders: Map[String, String]): B = {
    newInstance(urlFormatter, queryParams, Some(headers.get ++ givenHeaders), followsRedirects)
  }

  def followsRedirect(followRedirect: Boolean): B = {
    newInstance(urlFormatter, queryParams, headers, Some(followRedirect))
  }

  def asJSON(): B = {
    newInstance(urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_JSON) + (CONTENT_TYPE -> APPLICATION_JSON)), followsRedirects)
  }

  def asXML(): B = {
    newInstance(urlFormatter, queryParams, Some(headers.get + (ACCEPT -> APPLICATION_XML) + (CONTENT_TYPE -> APPLICATION_XML)), followsRedirects)
  }

  def getMethod: String

  def build(context: Context): Request = {

    requestBuilder setUrl urlFormatter.get.apply(context) setMethod getMethod setFollowRedirects followsRedirects.getOrElse(false)

    addCookiesTo(requestBuilder, context)
    addQueryParamsTo(requestBuilder, context)
    addHeadersTo(requestBuilder, headers)
    val request = requestBuilder build

    logger.debug("Built {} Request: {})", getMethod, request.getCookies)
    request
  }

  private def addCookiesTo(requestBuilder: RequestBuilder, context: Context) = {
    for (cookie <- context.getCookies) { requestBuilder.addOrReplaceCookie(cookie) }
  }

  private def addQueryParamsTo(requestBuilder: RequestBuilder, context: Context) = {
    requestBuilder setQueryParameters (new FluentStringsMap)
    for (queryParam <- queryParams.get) {
      queryParam._2 match {
        case StringParam(string) => requestBuilder addQueryParameter (queryParam._1, string)
        case ContextParam(string) => requestBuilder addQueryParameter (queryParam._1, context.getAttribute(string))
      }
    }
  }

  private def addHeadersTo(requestBuilder: RequestBuilder, headers: Option[Map[String, String]]) = {
    requestBuilder setHeaders (new FluentCaseInsensitiveStringsMap)
    for (header <- headers.get) { requestBuilder addHeader (header._1, header._2) }
  }

}