package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpHeaderCheck
import com.excilys.ebi.gatling.http.request.HttpPhase._

import org.apache.commons.lang3.StringUtils

object HttpHeaderCheckBuilder {
  class HttpHeaderCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String])
      extends HttpCheckBuilder[HttpHeaderCheckBuilder](expressionFormatter, expected, attrKey, Some(HeadersReceived)) {

    def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpHeaderCheckBuilder(expressionFormatter, expected, attrKey)
    }

    def build: HttpCheck =
      new HttpHeaderCheck(expressionFormatter.get, expected.get, attrKey.get)
  }

  def checkHeaderEquals(headerName: String, expected: String) = new HttpHeaderCheckBuilder(Some((c: Context) => headerName), Some(expected), Some(StringUtils.EMPTY))
  def checkHeaderEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(expected), Some(StringUtils.EMPTY))
  def checkHeaderExists(headerName: String) = new HttpHeaderCheckBuilder(Some((c: Context) => headerName), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY))
  def checkHeaderExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY))
}