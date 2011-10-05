package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType
import com.excilys.ebi.gatling.core.processor.builtin.EqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonEqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.ExistenceCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonExistenceCheckType
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpRegExpCheck
import org.apache.commons.lang3.StringUtils

object HttpRegExpCheckBuilder {
  def regexpEquals(expressionFormatter: Context => String, expected: String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(EqualityCheckType))
  def regexpEquals(expression: String, expected: String): HttpRegExpCheckBuilder = regexpEquals((c: Context) => expression, expected)

  def regexpNotEquals(expressionFormatter: Context => String, expected: String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(NonEqualityCheckType))
  def regexpNotEquals(expression: String, expected: String): HttpRegExpCheckBuilder = regexpNotEquals((c: Context) => expression, expected)

  def regexpExists(expressionFormatter: Context => String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(ExistenceCheckType))
  def regexpExists(expression: String): HttpRegExpCheckBuilder = regexpExists((c: Context) => expression)

  def regexpNotExists(expressionFormatter: Context => String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(NonExistenceCheckType))
  def regexpNotExists(expression: String): HttpRegExpCheckBuilder = regexpNotExists((c: Context) => expression)
}
class HttpRegExpCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType])
    extends HttpCheckBuilder[HttpRegExpCheckBuilder](expressionFormatter, expected, attrKey, httpPhase, checkType) {

  def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
    new HttpRegExpCheckBuilder(expressionFormatter, expected, attrKey, httpPhase, checkType)
  }

  def build: HttpCheck = new HttpRegExpCheck(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get, checkType.get)
}
