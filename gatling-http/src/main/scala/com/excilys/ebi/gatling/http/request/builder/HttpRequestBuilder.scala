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

import java.io.File

abstract class HttpRequestBuilder(val urlFormatter: Option[Context => String], val queryParams: Option[Map[String, Param]], val params: Option[Map[String, Param]],
                                  val headers: Option[Map[String, String]], val body: Option[HttpRequestBody],
                                  val followsRedirects: Option[Boolean])
    extends Logging {
  val requestBuilder = new RequestBuilder

  def withQueryParam(paramKey: String, paramValue: String): HttpRequestBuilder

  def withQueryParam(paramKey: String, paramValue: FromContext): HttpRequestBuilder

  def withQueryParam(paramKey: String): HttpRequestBuilder

  def withHeader(header: Tuple2[String, String]): HttpRequestBuilder

  def withHeaders(headers: Map[String, String]): HttpRequestBuilder

  def followsRedirect(followRedirect: Boolean): HttpRequestBuilder

  def asJSON(): HttpRequestBuilder

  def asXML(): HttpRequestBuilder

  def build(context: Context): Request

  def build(context: Context, method: String): Request = {

    requestBuilder setUrl urlFormatter.get.apply(context) setMethod method setFollowRedirects followsRedirects.getOrElse(false)

    addCookiesTo(requestBuilder, context)
    addQueryParamsTo(requestBuilder, context)
    addHeadersTo(requestBuilder, headers)
    val request = requestBuilder build

    logger.debug("Built {} Request: {})", method, request.getCookies)
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