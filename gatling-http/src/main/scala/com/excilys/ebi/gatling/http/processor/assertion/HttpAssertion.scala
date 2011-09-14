package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.processor.HttpProcessor

trait HttpAssertion extends HttpProcessor {
  def getAssertionType: AssertionType
  def getExpected: Any
}