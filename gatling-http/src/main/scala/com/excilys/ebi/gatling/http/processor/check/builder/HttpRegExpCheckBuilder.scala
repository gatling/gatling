package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpRegExpCheck

import org.apache.commons.lang3.StringUtils

object HttpRegExpCheckBuilder {
  class HttpRegExpCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
      extends HttpCheckBuilder[HttpRegExpCheckBuilder](expressionFormatter, expected, attrKey, httpPhase) {

    def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpRegExpCheckBuilder(expressionFormatter, expected, attrKey, httpPhase)
    }

    def build: HttpCheck =
      new HttpRegExpCheck(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get)
  }

  def checkRegexpEquals(expression: String, expected: String) = new HttpRegExpCheckBuilder(Some((c: Context) => expression), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def checkRegexpEquals(expressionFormatter: Context => String, expected: String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def checkRegexpExists(expression: String) = new HttpRegExpCheckBuilder(Some((c: Context) => expression), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def checkRegexpExists(expressionFormatter: Context => String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
}