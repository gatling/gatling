package com.excilys.ebi.gatling.http.request.builder

import com.ning.http.client.Request
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.feeder.Feeder

import com.excilys.ebi.gatling.http.request.Param

abstract class HttpRequestBuilder(val url: Option[String], val queryParams: Option[Map[String, Param]], val feeder: Option[Feeder]) {

  def withQueryParam(paramKey: String, paramValue: String): HttpRequestBuilder

  def withQueryParam(paramKey: String, paramValue: FromContext): HttpRequestBuilder

  def withFeeder(feeder: Feeder): HttpRequestBuilder

  def build(context: Context): Request
}