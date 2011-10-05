package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType
import com.excilys.ebi.gatling.core.processor.builtin.EqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonEqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.ExistenceCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonExistenceCheckType
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpXPathCheck
import org.apache.commons.lang3.StringUtils

object HttpXPathCheckBuilder {
  def xpathEquals(expressionFormatter: Context => String, expected: String) = new HttpXPathCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(EqualityCheckType))
  def xpathEquals(expression: String, expected: String): HttpXPathCheckBuilder = xpathEquals((c: Context) => expression, expected)

  def xpathNotEquals(expressionFormatter: Context => String, expected: String) = new HttpXPathCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(NonEqualityCheckType))
  def xpathNotEquals(expression: String, expected: String): HttpXPathCheckBuilder = xpathNotEquals((c: Context) => expression, expected)

  def xpathExists(expressionFormatter: Context => String) = new HttpXPathCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(ExistenceCheckType))
  def xpathExists(expression: String): HttpXPathCheckBuilder = xpathExists((c: Context) => expression)

  def xpathNotExists(expressionFormatter: Context => String) = new HttpXPathCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(NonExistenceCheckType))
  def xpathNotExists(expression: String): HttpXPathCheckBuilder = xpathNotExists((c: Context) => expression)
}
class HttpXPathCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType])
    extends HttpCheckBuilder[HttpXPathCheckBuilder](expressionFormatter, expected, attrKey, httpPhase, checkType) {

  def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
    new HttpXPathCheckBuilder(expressionFormatter, expected, attrKey, httpPhase, checkType)
  }

  def build = new HttpXPathCheck(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get, checkType.get)
}
