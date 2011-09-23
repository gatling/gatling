package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpXPathCheck

import org.apache.commons.lang3.StringUtils

object HttpXPathCheckBuilder {
  class HttpXPathCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
      extends HttpCheckBuilder[HttpXPathCheckBuilder](expressionFormatter, expected, attrKey, httpPhase) {

    def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpXPathCheckBuilder(expressionFormatter, expected, attrKey, httpPhase)
    }

    def build = new HttpXPathCheck(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get)
  }

  def checkXpathEquals(expression: String, expected: String) = new HttpXPathCheckBuilder(Some((c: Context) => expression), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def checkXpathEquals(expressionFormatter: Context => String, expected: String) = new HttpXPathCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def checkXpathExists(expression: String) = new HttpXPathCheckBuilder(Some((c: Context) => expression), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def checkXpathExists(expressionFormatter: Context => String) = new HttpXPathCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
}