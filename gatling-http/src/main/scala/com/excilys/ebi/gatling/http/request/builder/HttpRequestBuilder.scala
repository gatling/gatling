package com.excilys.ebi.gatling.http.request.builder

import com.ning.http.client.Request
import com.ning.http.client.RequestBuilder

import org.fusesource.scalate._

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody

import java.io.File

abstract class HttpRequestBuilder(val url: Option[String], val queryParams: Option[Map[String, Param]], val feeder: Option[Feeder]) extends Logging {

  def withQueryParam(paramKey: String, paramValue: String): HttpRequestBuilder

  def withQueryParam(paramKey: String, paramValue: FromContext): HttpRequestBuilder

  def withQueryParam(paramKey: String): HttpRequestBuilder

  def withFeeder(feeder: Feeder): HttpRequestBuilder

  def build(context: Context): Request

  def withHeader(header: Tuple2[String, String]): HttpRequestBuilder

  def asJSON: HttpRequestBuilder

  def asXML: HttpRequestBuilder

  private[builder] def compileBody(tplPath: String, values: Map[String, String]): String = {

    val engine = new TemplateEngine
    engine.allowCaching = false

    var bindings: List[Binding] = List()

    for (value <- values) {
      bindings = Binding(value._1, "String") :: bindings
    }

    logger.debug("Bindings: {}", bindings)

    engine.bindings = bindings
    engine.layout("user-templates/" + tplPath + ".ssp", values)
  }

  private[builder] def consumeSeed(feeder: Option[Feeder], context: Context) = feeder.map { f => context.setAttributes(f.next) }

  private[builder] def addCookiesTo(requestBuilder: RequestBuilder, context: Context) = for (cookie <- context.getCookies) { requestBuilder.addCookie(cookie) }

  private[builder] def addQueryParamsTo(requestBuilder: RequestBuilder, context: Context) = {
    for (queryParam <- queryParams.get) {
      queryParam._2 match {
        case StringParam(string) => requestBuilder addQueryParameter (queryParam._1, string)
        case ContextParam(string) => requestBuilder addQueryParameter (queryParam._1, context.getAttribute(string))
      }
    }
  }

  private[builder] def addHeadersTo(requestBuilder: RequestBuilder, headers: Option[Map[String, String]]) = for (header <- headers.get) { requestBuilder addHeader (header._1, header._2) }

  private[builder] def addBodyTo(requestBuilder: RequestBuilder, body: Option[HttpRequestBody]) = {
    body match {
      case Some(thing) =>
        thing match {
          case FilePathBody(filePath) => requestBuilder setBody new File("user-requests/" + filePath)
          case StringBody(body) => requestBuilder setBody body
          case TemplateBody(tplPath, values) => requestBuilder setBody compileBody(tplPath, values)
          case _ =>
        }
      case None =>
    }
  }

}