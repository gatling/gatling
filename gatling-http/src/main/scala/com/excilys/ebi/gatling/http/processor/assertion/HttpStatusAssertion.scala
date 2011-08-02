package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.RangeAssertionProvider

import com.excilys.ebi.gatling.http.phase.StatusReceived

class HttpStatusAssertion(expected: String, attrKey: Option[String])
    extends HttpAssertion("", expected, attrKey, new StatusReceived, new RangeAssertionProvider) {

  override def toString = "HttpStatusAssertion (Http Response Status must be in '{" + expected + "}')"
}