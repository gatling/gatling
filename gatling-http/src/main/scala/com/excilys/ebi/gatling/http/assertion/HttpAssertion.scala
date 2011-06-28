package com.excilys.ebi.gatling.http.assertion

import com.excilys.ebi.gatling.core.assertion.AbstractAssertion

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase.HttpResponseHook

abstract class HttpAssertion(val expected: Any, httpHook: HttpResponseHook, val provider: AbstractAssertion)
  extends HttpProcessor(httpHook)