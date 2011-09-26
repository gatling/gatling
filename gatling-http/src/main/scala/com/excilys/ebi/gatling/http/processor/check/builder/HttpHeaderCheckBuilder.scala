package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType._

import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpHeaderCheck
import com.excilys.ebi.gatling.http.request.HttpPhase._

import org.apache.commons.lang3.StringUtils

object HttpHeaderCheckBuilder {
  class HttpHeaderCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], checkType: Option[CheckType])
      extends HttpCheckBuilder[HttpHeaderCheckBuilder](expressionFormatter, expected, attrKey, Some(HeadersReceived), checkType) {

    def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
      new HttpHeaderCheckBuilder(expressionFormatter, expected, attrKey, checkType)
    }

    def build: HttpCheck =
      new HttpHeaderCheck(expressionFormatter.get, expected.get, attrKey.get, checkType.get)
  }

  def checkHeaderEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(expected), Some(StringUtils.EMPTY), Some(EQUALITY))
  def checkHeaderEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = checkHeaderEquals((c: Context) => headerName, expected)

  def checkHeaderNotEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(expected), Some(StringUtils.EMPTY), Some(INEQUALITY))
  def checkHeaderNotEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = checkHeaderNotEquals((c: Context) => headerName, expected)

  def checkHeaderExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(EXISTENCE))
  def checkHeaderExists(headerName: String): HttpHeaderCheckBuilder = checkHeaderExists((c: Context) => headerName)

  def checkHeaderNotExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(INEXISTENCE))
  def checkHeaderNotExists(headerName: String): HttpHeaderCheckBuilder = checkHeaderNotExists((c: Context) => headerName)
}