package com.excilys.ebi.gatling.http.request.builder.mixin

import com.ning.http.client.Request
import com.ning.http.client.RequestBuilder
import com.ning.http.client.FluentStringsMap

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext

import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBuilder

trait RequestParamsAndBody extends Body {

  abstract override def build(context: Context, method: String): Request = {
    //requestBuilder setMethod method
    addParamsTo(requestBuilder, params, context)
    super.build(context, method)
  }

  def withParam(paramKey: String, paramValue: String): HttpRequestBuilder

  def withParam(paramKey: String, paramValue: FromContext): HttpRequestBuilder

  def withParam(paramKey: String): HttpRequestBuilder

  private def addParamsTo(requestBuilder: RequestBuilder, params: Option[Map[String, Param]], context: Context) = {
    requestBuilder setParameters new FluentStringsMap
    for (param <- params.get) {
      param._2 match {
        case StringParam(string) => requestBuilder addParameter (param._1, string)
        case ContextParam(string) => requestBuilder addParameter (param._1, context.getAttribute(string))
      }
    }
  }
}