package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.MIMEType._
import com.excilys.ebi.gatling.http.request.HttpRequestBody

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object GetHttpRequestBuilder {
  class GetHttpRequestBuilder(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
                              headers: Option[Map[String, String]], followsRedirects: Option[Boolean])
      extends AbstractHttpRequestBuilder[GetHttpRequestBuilder](urlFormatter, queryParams, headers, followsRedirects) with Logging {

    def newInstance(urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], followsRedirects: Option[Boolean]) = {
      new GetHttpRequestBuilder(urlFormatter, queryParams, headers, followsRedirects)
    }

    def getMethod = "GET"
  }

  def get(url: String, interpolations: String*) = new GetHttpRequestBuilder(Some((c: Context) => interpolate(c, url, interpolations)), Some(Map()), Some(Map()), None)
  def get(f: Context => String) = new GetHttpRequestBuilder(Some(f), Some(Map()), Some(Map()), None)
}