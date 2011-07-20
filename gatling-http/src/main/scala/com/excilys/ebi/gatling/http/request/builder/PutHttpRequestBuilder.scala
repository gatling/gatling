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
import com.excilys.ebi.gatling.http.request.FeederParam
import com.excilys.ebi.gatling.http.request.ContextParam

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import java.io.File

import org.fusesource.scalate._

object PutHttpRequestBuilder {
  class PutHttpRequestBuilder(url: Option[String], queryParams: Option[Map[String, Param]], val headers: Option[Map[String, String]],
                              val body: Option[HttpRequestBody], feeder: Option[Feeder])
      extends HttpRequestBuilder(url, queryParams, feeder) with Logging {
    def withQueryParam(paramKey: String, paramValue: String) = new PutHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))), headers, body, feeder)

    def withQueryParam(paramKey: String, paramValue: FromContext) = new PutHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> ContextParam(paramValue.attributeKey))), headers, body, feeder)

    def withQueryParam(paramKey: String) = withQueryParam(paramKey, FromContext(paramKey))

    def withHeader(header: Tuple2[String, String]) = new PutHttpRequestBuilder(url, queryParams, Some(headers.get + (header._1 -> header._2)), body, feeder)

    def asJSON = new PutHttpRequestBuilder(url, queryParams, Some(headers.get + ("Accept" -> "application/json") + ("Content-Type" -> "application/json")), body, feeder)

    def asXML = new PutHttpRequestBuilder(url, queryParams, Some(headers.get + ("Accept" -> "application/xml") + ("Content-Type" -> "application/xml")), body, feeder)

    def withFile(filePath: String) = new PutHttpRequestBuilder(url, queryParams, headers, Some(FilePathBody(filePath)), feeder)

    def withBody(body: String) = new PutHttpRequestBuilder(url, queryParams, headers, Some(StringBody(body)), feeder)

    def withTemplateBody(tplPath: String, values: Map[String, String]) = new PutHttpRequestBuilder(url, queryParams, headers, Some(TemplateBody(tplPath, values)), feeder)

    def withFeeder(feeder: Feeder) = new PutHttpRequestBuilder(url, queryParams, headers, body, Some(feeder))

    def build(context: Context): Request = {
      feeder.map { f =>
        context.setAttributes(f.next)
      }

      val requestBuilder = new RequestBuilder setUrl url.get setMethod "PUT"

      for (cookie <- context.getCookies) {
        requestBuilder.addCookie(cookie)
      }

      for (queryParam <- queryParams.get) {
        queryParam._2 match {
          case StringParam(string) => requestBuilder addQueryParameter (queryParam._1, string)
          case ContextParam(string) => requestBuilder addQueryParameter (queryParam._1, context.getAttribute(string))
        }
      }

      for (header <- headers.get) requestBuilder addHeader (header._1, header._2)

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

      logger.debug("Built PUT Request")
      requestBuilder build
    }

    private def compileBody(tplPath: String, values: Map[String, String]): String = {

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
  }

  def put(url: String) = new PutHttpRequestBuilder(Some(url), Some(Map()), Some(Map()), None, None)
}