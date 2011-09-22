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

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import java.io.File

import org.fusesource.scalate._

object PostHttpRequestBuilder {
  class PostHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], params: Option[Map[String, Param]],
                               headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean])
      extends AbstractHttpRequestWithBodyAndParamsBuilder[PostHttpRequestBuilder](urlFormatter, queryParams, params, headers, body, followsRedirects) {

    def newInstance(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], params: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean]) = {
      new PostHttpRequestBuilder(urlFormatter, queryParams, params, headers, body, followsRedirects)
    }

    def getMethod = "POST"
  }

  def post(url: String, interpolations: String*) = new PostHttpRequestBuilder(Some((c: Context) => interpolate(c, url, interpolations)), Some(Map()), Some(Map()), Some(Map()), None, None)
  def post(f: Context => String) = new PostHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), Some(Map()), None, None)
}