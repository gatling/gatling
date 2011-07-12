package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FileBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import java.io.File

object PostHttpRequestBuilder {
  class PostHttpRequestBuilder(val url: Option[String], val queryParams: Option[Map[String, String]], val params: Option[Map[String, String]],
    val headers: Option[Map[String, String]], val body: Option[HttpRequestBody])
    extends HttpRequestBuilder with Logging {

    def withQueryParam(queryParam: Tuple2[String, String]) = new PostHttpRequestBuilder(url, Some(queryParams.get + (queryParam._1 -> queryParam._2)), params, headers, body)

    def withParam(param: Tuple2[String, String]) = new PostHttpRequestBuilder(url, queryParams, Some(params.get + (param._1 -> param._2)), headers, body)

    def withHeader(header: Tuple2[String, String]) = new PostHttpRequestBuilder(url, queryParams, params, Some(headers.get + (header._1 -> header._2)), body)

    def asJSON = new PostHttpRequestBuilder(url, queryParams, params, Some(headers.get + ("Accept" -> "application/json")), body)

    def asXML = new PostHttpRequestBuilder(url, queryParams, params, Some(headers.get + ("Accept" -> "application/xml")), body)

    def withFile(file: File) = new PostHttpRequestBuilder(url, queryParams, params, headers, Some(FileBody(file)))

    def withFilePath(filePath: String) = new PostHttpRequestBuilder(url, queryParams, params, headers, Some(FilePathBody(filePath)))

    def withBody(body: String) = new PostHttpRequestBuilder(url, queryParams, params, headers, Some(StringBody(body)))

    def build(): Request = {
      val requestBuilder = new RequestBuilder setUrl url.get setMethod "POST"

      for (queryParam <- queryParams.get) requestBuilder addQueryParameter (queryParam._1, queryParam._2)

      for (param <- params.get) requestBuilder addParameter (param._1, param._2)

      for (header <- headers.get) requestBuilder addHeader (header._1, header._2)

      body match {
        case Some(thing) =>
          thing match {
            case FileBody(file) =>
            case FilePathBody(filePath) =>
            case StringBody(body) => requestBuilder setBody body
            case _ =>
          }
        case None =>
      }

      logger.debug("Built POST Request")
      requestBuilder build
    }
  }

  def post(url: String) = new PostHttpRequestBuilder(Some(url), Some(Map()), Some(Map()), Some(Map()), None)
}