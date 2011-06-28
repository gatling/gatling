package com.excilys.ebi.gatling.http.assertion

import com.excilys.ebi.gatling.core.provider.assertion.AbstractAssertionProvider

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase.HttpResponseHook

abstract class HttpAssertion(val expected: Any, httpHook: HttpResponseHook, val provider: AbstractAssertionProvider)
  extends HttpProcessor(httpHook)