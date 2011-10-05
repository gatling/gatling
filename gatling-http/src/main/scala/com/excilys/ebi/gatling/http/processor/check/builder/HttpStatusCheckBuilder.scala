package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpStatusCheck

import org.apache.commons.lang3.StringUtils

object HttpStatusCheckBuilder {
  def statusInRange(range: Range) = new HttpStatusCheckBuilder(Some(range.mkString(":")), Some(StringUtils.EMPTY))
  def status(status: Int) = new HttpStatusCheckBuilder(Some(status.toString), Some(StringUtils.EMPTY))
}
class HttpStatusCheckBuilder(expected: Option[String], attrKey: Option[String])
    extends HttpCheckBuilder[HttpStatusCheckBuilder](None, expected, attrKey, Some(StatusReceived), None) {

  def newInstance(expression: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
    new HttpStatusCheckBuilder(expected, attrKey)
  }

  def build: HttpCheck = new HttpStatusCheck(expected.get, attrKey.get)
}
