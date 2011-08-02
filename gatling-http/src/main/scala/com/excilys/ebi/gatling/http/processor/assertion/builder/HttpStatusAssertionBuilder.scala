package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.phase.StatusReceived
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpStatusAssertion

object HttpStatusAssertionBuilder {
  class HttpStatusAssertionBuilder(expected: Option[String], attrKey: Option[String])
      extends HttpAssertionBuilder(None, expected, attrKey, Some(new StatusReceived)) {
    def in(attrKey: String) = new HttpStatusAssertionBuilder(expected, Some(attrKey))

    def build: HttpAssertion = new HttpStatusAssertion(expected.get, attrKey)
  }

  def assertStatusInRange(range: Range) = new HttpStatusAssertionBuilder(Some(range.mkString(":")), None)
  def assertStatus(status: Int) = new HttpStatusAssertionBuilder(Some(status.toString), None)
}