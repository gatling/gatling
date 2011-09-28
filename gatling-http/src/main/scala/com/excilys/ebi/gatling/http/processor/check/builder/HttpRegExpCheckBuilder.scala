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
  class HttpRegExpCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType])
    extends HttpCheckBuilder[HttpRegExpCheckBuilder](expressionFormatter, expected, attrKey, httpPhase, checkType) {

    def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
      new HttpRegExpCheckBuilder(expressionFormatter, expected, attrKey, httpPhase, checkType)
    }

    def build: HttpCheck = new HttpRegExpCheck(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get, checkType.get)
  }

  def checkRegexpEquals(expressionFormatter: Context => String, expected: String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(EqualityCheckType))
  def checkRegexpEquals(expression: String, expected: String): HttpRegExpCheckBuilder = checkRegexpEquals((c: Context) => expression, expected)

  def checkRegexpNotEquals(expressionFormatter: Context => String, expected: String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(NonEqualityCheckType))
  def checkRegexpNotEquals(expression: String, expected: String): HttpRegExpCheckBuilder = checkRegexpNotEquals((c: Context) => expression, expected)

  def checkRegexpExists(expressionFormatter: Context => String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(ExistenceCheckType))
  def checkRegexpExists(expression: String): HttpRegExpCheckBuilder = checkRegexpExists((c: Context) => expression)

  def checkRegexpNotExists(expressionFormatter: Context => String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived), Some(NonExistenceCheckType))
  def checkRegexpNotExists(expression: String): HttpRegExpCheckBuilder = checkRegexpNotExists((c: Context) => expression)
}