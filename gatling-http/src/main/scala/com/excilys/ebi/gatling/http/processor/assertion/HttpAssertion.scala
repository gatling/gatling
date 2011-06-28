package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.AbstractAssertionProvider

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase.HttpPhase

abstract class HttpAssertion(val expected: Any, httpPhase: HttpPhase, val provider: AbstractAssertionProvider)
  extends HttpProcessor(httpPhase)