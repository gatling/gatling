package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.FeederParam

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import java.io.File

import org.fusesource.scalate._

object PostHttpRequestBuilder {
  class PostHttpRequestBuilder(val url: Option[String], val queryParams: Option[Map[String, Param]], val params: Option[Map[String, String]],
    val headers: Option[Map[String, String]], val body: Option[HttpRequestBody])
    extends HttpRequestBuilder with Logging {

    def withQueryParam(paramKey: String, paramValue: String) = new PostHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> StringParam(paramValue))), params, headers, body)

    def withQueryParam(paramKey: String, paramValue: Function[Int, String]) = new PostHttpRequestBuilder(url, Some(queryParams.get + (paramKey -> FeederParam(paramValue))), params, headers, body)

    def withParam(param: Tuple2[String, String]) = new PostHttpRequestBuilder(url, queryParams, Some(params.get + (param._1 -> param._2)), headers, body)

    def withHeader(header: Tuple2[String, String]) = new PostHttpRequestBuilder(url, queryParams, params, Some(headers.get + (header._1 -> header._2)), body)

    def asJSON = new PostHttpRequestBuilder(url, queryParams, params, Some(headers.get + ("Accept" -> "application/json") + ("Content-Type" -> "application/json")), body)

    def asXML = new PostHttpRequestBuilder(url, queryParams, params, Some(headers.get + ("Accept" -> "application/xml") + ("Content-Type" -> "application/xml")), body)

    def withFile(filePath: String) = new PostHttpRequestBuilder(url, queryParams, params, headers, Some(FilePathBody(filePath)))

    def withBody(body: String) = new PostHttpRequestBuilder(url, queryParams, params, headers, Some(StringBody(body)))

    def withTemplateBody(tplPath: String, values: Map[String, String]) = new PostHttpRequestBuilder(url, queryParams, params, headers, Some(TemplateBody(tplPath, values)))

    def build(feederIndex: Int): Request = {
      val requestBuilder = new RequestBuilder setUrl url.get setMethod "POST"

      for (queryParam <- queryParams.get) {
        queryParam._2 match {
          case StringParam(string) => requestBuilder addQueryParameter (queryParam._1, string)
          case FeederParam(func) => requestBuilder addQueryParameter (queryParam._1, func(feederIndex))
        }
      }

      for (param <- params.get) requestBuilder addParameter (param._1, param._2)

      for (header <- headers.get) requestBuilder addHeader (header._1, header._2)

      body match {
        case Some(thing) =>
          thing match {
            case FilePathBody(filePath) => requestBuilder setBody new File("request-files/" + filePath)
            case StringBody(body) => requestBuilder setBody body
            case TemplateBody(tplPath, values) => requestBuilder setBody compileBody(tplPath, values)
            case _ =>
          }
        case None =>
      }

      logger.debug("Built POST Request")
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

  def post(url: String) = new PostHttpRequestBuilder(Some(url), Some(Map()), Some(Map()), Some(Map()), None)
}