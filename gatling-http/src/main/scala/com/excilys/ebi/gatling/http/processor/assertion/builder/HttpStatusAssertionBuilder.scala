package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpStatusAssertion

import org.apache.commons.lang3.StringUtils

object HttpStatusAssertionBuilder {
  class HttpStatusAssertionBuilder(expected: Option[String], attrKey: Option[String])
      extends HttpAssertionBuilder[HttpStatusAssertionBuilder](None, expected, attrKey, Some(StatusReceived)) {

    def newInstance(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpStatusAssertionBuilder(expected, attrKey)
    }

    def build: HttpAssertion = new HttpStatusAssertion(expected.get, attrKey.get)
  }

  def assertStatusInRange(range: Range) = new HttpStatusAssertionBuilder(Some(range.mkString(":")), Some(StringUtils.EMPTY))
  def assertStatus(status: Int) = new HttpStatusAssertionBuilder(Some(status.toString), Some(StringUtils.EMPTY))
}