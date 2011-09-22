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

object PutHttpRequestBuilder {
  class PutHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
                              headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
      extends AbstractHttpRequestWithBodyBuilder[PutHttpRequestBuilder](urlFormatter, queryParams, headers, body, followsRedirects) {

    def newInstance(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]) = {
      new PutHttpRequestBuilder(urlFormatter, queryParams, headers, body, followsRedirects)
    }

    def getMethod = "PUT"
  }

  def put(url: String, interpolations: String*) = new PutHttpRequestBuilder(Some((c: Context) => interpolate(c, url, interpolations)), Some(Map()), Some(Map()), None, None)
  def put(f: Context => String) = new PutHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), None, None)
}